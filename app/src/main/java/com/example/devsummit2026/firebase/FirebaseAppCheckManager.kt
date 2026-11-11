package com.example.devsummit2026.firebase

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object FirebaseAppCheckManager {
    fun initialize(isDebug: Boolean) {
        if (isDebug) {
            val firebaseApp = Firebase.app
            val prefsName = "com.google.firebase.appcheck.debug.store.${firebaseApp.persistenceKey}"
            val sharedPreferences = firebaseApp.applicationContext.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit()
                .putString(
                    "com.google.firebase.appcheck.debug.DEBUG_SECRET",
                    "bc2f8ccf-be3b-41f3-b80e-4bf68733394e"
                )
                .apply()

            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
    }
}
