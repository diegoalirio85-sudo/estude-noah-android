package com.estudenoah.app.security

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class FirebaseAppCheckTokenProvider {
    fun getToken(forceRefresh: Boolean = false, callback: (Result<String>) -> Unit) {
        val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
        if (app == null) {
            callback(Result.failure(IllegalStateException("Firebase is not configured.")))
            return
        }
        FirebaseAppCheck.getInstance(app).getAppCheckToken(forceRefresh)
            .addOnSuccessListener { result -> callback(Result.success(result.token)) }
            .addOnFailureListener { error -> callback(Result.failure(error)) }
    }
}

