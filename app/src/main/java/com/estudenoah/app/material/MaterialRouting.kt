package com.estudenoah.app.material

import java.util.Locale

internal enum class MaterialRoute {
    LEGACY_PPT_BACKEND,
    LOCAL_EXTRACTION
}

internal object MaterialRouting {
    private const val LEGACY_PPT_MIME = "application/vnd.ms-powerpoint"

    fun route(displayName: String, mimeType: String?): MaterialRoute {
        return when (extensionOf(displayName)) {
            "ppt" -> MaterialRoute.LEGACY_PPT_BACKEND
            "pptx" -> MaterialRoute.LOCAL_EXTRACTION
            "" -> if (mimeType.equals(LEGACY_PPT_MIME, ignoreCase = true)) {
                MaterialRoute.LEGACY_PPT_BACKEND
            } else {
                MaterialRoute.LOCAL_EXTRACTION
            }
            else -> MaterialRoute.LOCAL_EXTRACTION
        }
    }

    inline fun <T> dispatch(
        displayName: String,
        mimeType: String?,
        legacyPpt: () -> T,
        localExtraction: () -> T
    ): T = when (route(displayName, mimeType)) {
        MaterialRoute.LEGACY_PPT_BACKEND -> legacyPpt()
        MaterialRoute.LOCAL_EXTRACTION -> localExtraction()
    }

    private fun extensionOf(displayName: String): String = displayName
        .trim()
        .substringAfterLast('.', "")
        .lowercase(Locale.ROOT)
}
