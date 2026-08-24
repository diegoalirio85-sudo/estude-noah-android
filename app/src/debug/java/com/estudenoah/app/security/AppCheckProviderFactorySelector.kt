package com.estudenoah.app.security

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

internal object AppCheckProviderFactorySelector {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}

