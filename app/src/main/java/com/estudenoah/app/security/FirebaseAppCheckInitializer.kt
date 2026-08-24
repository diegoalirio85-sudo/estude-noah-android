package com.estudenoah.app.security

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

object FirebaseAppCheckInitializer {
    fun initialize(context: Context): Boolean {
        val app = FirebaseApp.initializeApp(context) ?: return false
        FirebaseAppCheck.getInstance(app)
            .installAppCheckProviderFactory(AppCheckProviderFactorySelector.factory())
        return true
    }
}

