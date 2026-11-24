package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application de test Robolectric : évite d'instancier AppContainer (et Room disque)
 * au démarrage de chaque test unitaire, et réinitialise l'état statique FirebaseApp.
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.clearInstancesForTest()
        } catch (_: Exception) {
        }
    }
}
