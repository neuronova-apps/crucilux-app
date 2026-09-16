package com.neuronova.crucilux

import android.app.Application
import android.util.Log
import com.neuronova.crucilux.data.GameConfigProvider
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Clase de Aplicación de Crucilux.
 * Inicializa el repositorio local del banco maestro desde assets al inicio del proceso.
 */
class CruciluxApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val repository = CruciluxBankRepository.getInstance()
        GameConfigProvider.initialize(repository)

        applicationScope.launch {
            try {
                repository.loadFromAssets(applicationContext)
                Log.d(
                    "CruciluxApplication",
                    "Banco maestro v1.37 cargado exitosamente (${repository.getAllBoards().size} tableros)",
                )
            } catch (exception: Exception) {
                Log.e("CruciluxApplication", "Error cargando banco maestro desde assets", exception)
            }
        }
    }
}
