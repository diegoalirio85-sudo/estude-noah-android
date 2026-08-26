package com.estudenoah.app.security

import com.google.firebase.auth.FirebaseAuth

class FirebaseIdTokenProvider(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    fun getToken(forceRefresh: Boolean = false, callback: (Result<String>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            callback(Result.failure(BackendLoginRequiredException()))
            return
        }
        user.getIdToken(forceRefresh)
            .addOnSuccessListener { result ->
                val token = result.token
                if (token.isNullOrBlank()) callback(Result.failure(BackendLoginRequiredException()))
                else callback(Result.success(token))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }
}

class BackendLoginRequiredException : IllegalStateException("Firebase Authentication session is required.")

