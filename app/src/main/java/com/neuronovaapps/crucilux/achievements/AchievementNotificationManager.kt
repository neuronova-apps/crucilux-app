package com.neuronovaapps.crucilux.achievements

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Gestor centralizado de notificaciones de logros in-app para Crucilux.
 *
 * Responsabilidades:
 * 1. Garantizar presentación secuencial tipo FIFO sin superposición de banners.
 * 2. Mantener la visibilidad de cada logro durante [displayDurationMs] (~4 segundos).
 * 3. Permitir el descarte manual inmediato (vía botón de cierre o deslizamiento) cancelando el temporizador activo.
 * 4. Desacoplar el temporizador del ciclo de vida reactivo de Room y de Compose, impidiendo cancelaciones espurias.
 * 5. Garantizar la persistencia atómica en Room marcando la notificación como mostrada una vez iniciada la presentación.
 * 6. Limpiar estrictamente el estado visual (`activeAchievement = null`) al concluir cada banner o ante cancelación de corrutina.
 */
class AchievementNotificationManager(
    private val repository: AchievementRepository,
    private val scope: CoroutineScope,
    val displayDurationMs: Long = DEFAULT_DISPLAY_DURATION_MS,
    val transitionDelayMs: Long = DEFAULT_TRANSITION_DELAY_MS,
    autoStart: Boolean = true,
) {
    private val _activeAchievement = MutableStateFlow<AchievementState?>(null)
    val activeAchievement: StateFlow<AchievementState?> = _activeAchievement.asStateFlow()

    private val queue = ArrayDeque<AchievementState>()
    private val queuedOrProcessedIds = mutableSetOf<String>()
    private val mutex = Mutex()

    @Volatile
    private var dismissSignal: CompletableDeferred<Unit>? = null

    private var isProcessing = false
    private var processingJob: Job? = null
    private var collectionJob: Job? = null

    init {
        if (autoStart) {
            start()
        }
    }

    /**
     * Inicia la observación reactiva de logros desbloqueados no notificados en Room.
     */
    fun start() {
        if (collectionJob != null) return
        collectionJob = scope.launch {
            repository.observeUnnotifiedUnlocked().collect { unnotified ->
                enqueueAll(unnotified)
            }
        }
    }

    /**
     * Encola logros de forma segura, descartando duplicados ya procesados o en cola.
     */
    suspend fun enqueueAll(achievements: List<AchievementState>) {
        mutex.withLock {
            var added = false
            for (achievement in achievements) {
                if (achievement.id !in queuedOrProcessedIds) {
                    queuedOrProcessedIds.add(achievement.id)
                    queue.addLast(achievement)
                    added = true
                }
            }
            if (added && !isProcessing) {
                isProcessing = true
                processingJob = scope.launch {
                    processQueue()
                }
            }
        }
    }

    /**
     * Bucle secuencial FIFO de presentación de notificaciones.
     */
    private suspend fun processQueue() {
        try {
            while (true) {
                val nextAchievement = mutex.withLock {
                    val next = queue.removeFirstOrNull()
                    if (next == null) {
                        isProcessing = false
                    }
                    next
                } ?: break

                try {
                    // 1. Iniciar presentación visual efectiva
                    _activeAchievement.value = nextAchievement

                    // 2. Persistir en Room que la notificación ha iniciado su presentación efectiva
                    repository.markNotificationShown(nextAchievement.id)

                    // 3. Temporizador de visualización cancelable por descarte manual
                    val signal = CompletableDeferred<Unit>()
                    dismissSignal = signal

                    try {
                        withTimeoutOrNull(displayDurationMs) {
                            signal.await()
                        }
                    } finally {
                        dismissSignal = null
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    Log.e(TAG, "Error procesando notificación de logro", error)
                } finally {
                    // 4. Liberar estado visual al concluir la presentación
                    _activeAchievement.value = null
                }

                // 5. Breve pausa para permitir la animación de salida de Compose antes del siguiente
                if (transitionDelayMs > 0L) {
                    delay(transitionDelayMs)
                }
            }
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    isProcessing = false
                    dismissSignal = null
                }
                _activeAchievement.value = null
            }
        }
    }

    /**
     * Descarta manualmente la notificación en pantalla, cancelando el temporizador activo
     * y permitiendo avanzar de inmediato al siguiente logro en cola si lo hubiera.
     */
    fun dismissCurrent() {
        dismissSignal?.complete(Unit)
    }

    /**
     * Retorna la cantidad actual de logros en cola pendientes de ser mostrados.
     */
    suspend fun getQueuedCount(): Int = mutex.withLock { queue.size }

    /**
     * Reinicia el estado de sesión de notificaciones (útil para pruebas y reinicios).
     */
    suspend fun resetSession() {
        mutex.withLock {
            queue.clear()
            queuedOrProcessedIds.clear()
            dismissSignal?.complete(Unit)
            dismissSignal = null
            isProcessing = false
            _activeAchievement.value = null
        }
    }

    companion object {
        private const val TAG = "AchievementNotificationManager"
        const val DEFAULT_DISPLAY_DURATION_MS = 4000L
        const val DEFAULT_TRANSITION_DELAY_MS = 350L
    }
}
