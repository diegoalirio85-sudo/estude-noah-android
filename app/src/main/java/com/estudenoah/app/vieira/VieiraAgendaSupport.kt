package com.estudenoah.app.vieira

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object VieiraAgendaSupport {
    const val PORTAL_URL = "https://portal.asav.org.br/framehtml/web/app/edu/PortalEducacional/login/"

    fun isAllowedPortalUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        uri.scheme.equals("https", ignoreCase = true) &&
            (host == "asav.org.br" || host.endsWith(".asav.org.br"))
    }.getOrDefault(false)

    fun cleanCapturedText(raw: String): String {
        val normalized = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val result = mutableListOf<String>()
        var previousBlank = false
        normalized.lineSequence().forEach { line ->
            val clean = line.trim().replace(Regex("[\\t ]+"), " ")
            val blank = clean.isBlank()
            if (!blank || !previousBlank) result += clean
            previousBlank = blank
        }
        return result.joinToString("\n").trim()
    }

    fun defaultTitle(now: Date = Date()): String {
        val date = SimpleDateFormat("dd-MM-yyyy", Locale("pt", "BR")).format(now)
        return "Agenda Vieira - $date"
    }
}
