package com.estudenoah.app.network

import android.content.Context
import android.net.Uri
import com.estudenoah.app.domain.Question

internal class BackendActivityRepository(
    private val client: EstudeNoahBackendClient = EstudeNoahBackendClient(FirebaseTokenSource(), UrlConnectionBackendTransport())
) {
    suspend fun fromText(sourceType: String, title: String, subject: String, text: String): List<Question> =
        BackendActivityMapper.questions(client.fromText(sourceType, title, subject, DEFAULT_GRADE, text))

    suspend fun fromYoutube(url: String, subject: String): List<Question> =
        BackendActivityMapper.questions(client.fromYoutube(url, subject, DEFAULT_GRADE))

    suspend fun fromPpt(context: Context, uri: Uri, fileName: String, subject: String): List<Question> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > 50 * 1024 * 1024) throw BackendException(status = 413, message = "PPT too large.")
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw BackendException(message = "PPT could not be opened.")
        return BackendActivityMapper.questions(client.fromPpt(fileName, bytes, subject, DEFAULT_GRADE))
    }
}

