package com.estudenoah.app

import android.app.Application
import com.estudenoah.app.security.FirebaseAppCheckInitializer

class EstudeNoahApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAppCheckInitializer.initialize(this)
    }
}

