package com.neuronova.crucilux

import android.app.Application
import android.util.Log
import com.neuronova.crucilux.data.GameConfigProvider
import com.neuronova.crucilux.data.bank.CruciluxBankRepository

/**
 * Clase de Aplicación de Crucilux.
 * Inicializa el repositorio local del banco maestro desde assets al inicio del proceso.
 */
class CruciluxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val repository = CruciluxBankRepository.getInstance()
            repository.loadFromAssets(applicationContext)
            GameConfigProvider.initialize(repository)
            Log.d("CruciluxApplication", "Banco maestro v1.28 cargado exitosamente (${repository.getAllBoards().size} tableros)")
        } catch (e: Exception) {
            Log.e("CruciluxApplication", "Error cargando banco maestro desde assets", e)
        }
    }
}
