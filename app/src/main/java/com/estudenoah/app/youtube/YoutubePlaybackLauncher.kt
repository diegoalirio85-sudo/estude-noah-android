package com.estudenoah.app.youtube

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI
import java.util.Locale

internal object YoutubePlaybackLauncher {
    internal const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    private val allowedHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "youtu.be"
    )

    fun supportedUrl(raw: String): String? {
        val candidate = raw.trim()
        if (candidate.isBlank()) return null
        return runCatching {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host?.lowercase(Locale.ROOT)
            candidate.takeIf {
                scheme == "https" &&
                    host in allowedHosts &&
                    uri.userInfo == null
            }
        }.getOrNull()
    }

    fun open(context: Context, rawUrl: String): Boolean {
        val url = supportedUrl(rawUrl) ?: return false
        val uri = Uri.parse(url)

        val youtubeIntent = playbackIntent(context, uri).apply {
            setPackage(YOUTUBE_PACKAGE)
        }
        try {
            context.startActivity(youtubeIntent)
            return true
        } catch (_: ActivityNotFoundException) {
            // O app oficial não está instalado: usa o resolvedor padrão como fallback.
        }

        return try {
            context.startActivity(playbackIntent(context, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun playbackIntent(context: Context, uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
