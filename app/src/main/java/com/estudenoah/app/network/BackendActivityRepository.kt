package com.estudenoah.app.network

import android.content.Context
import android.net.Uri
import com.estudenoah.app.domain.Question

internal data class ResolvedSchoolActivity(
    val title: String,
    val youtubeUrl: String,
    val questions: List<Question>
)

internal class BackendActivityRepository(
    private val client: EstudeNoahBackendClient = EstudeNoahBackendClient(FirebaseTokenSource(), UrlConnectionBackendTransport()),
    private val schoolUrlClient: SchoolMaterialUrlClient = SchoolMaterialUrlClient()
) {
    suspend fun fromText(sourceType: String, title: String, subject: String, text: String): List<Question> =
        BackendActivityMapper.questions(client.fromText(sourceType, title, subject, DEFAULT_GRADE, text))

    suspend fun fromYoutube(url: String, subject: String): List<Question> =
        BackendActivityMapper.questions(client.fromYoutube(url, subject, DEFAULT_GRADE))

    suspend fun fromSchoolUrl(url: String, subject: String): ResolvedSchoolActivity {
        val resolved = schoolUrlClient.resolve(url)
        val questions = fromYoutube(resolved.resolvedUrl, subject)
        return ResolvedSchoolActivity(
            title = resolved.title,
            youtubeUrl = resolved.resolvedUrl,
            questions = questions
        )
    }

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

