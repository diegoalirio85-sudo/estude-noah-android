package com.estudenoah.app.network

import com.estudenoah.app.BuildConfig
import com.estudenoah.app.security.FirebaseAppCheckTokenProvider
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val APP_CHECK_HEADER = "X-Firebase-AppCheck"
internal const val DEFAULT_GRADE = "4º Ano Ensino Fundamental"

internal fun interface AppCheckTokenSource {
    suspend fun token(forceRefresh: Boolean): String
}

internal fun interface BackendTransport {
    suspend fun execute(request: BackendRequest): BackendResponse
}

internal class FirebaseTokenSource(
    private val provider: FirebaseAppCheckTokenProvider = FirebaseAppCheckTokenProvider()
) : AppCheckTokenSource {
    override suspend fun token(forceRefresh: Boolean): String = suspendCancellableCoroutine { continuation ->
        provider.getToken(forceRefresh) { result ->
            if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
        }
    }
}

internal class UrlConnectionBackendTransport(
    private val baseUrl: String = BuildConfig.ESTUDE_NOAH_BACKEND_BASE_URL,
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 300_000
) : BackendTransport {
    override suspend fun execute(request: BackendRequest): BackendResponse {
        try {
            val connection = URL(baseUrl.trimEnd('/') + request.path).openConnection() as HttpURLConnection
            connection.requestMethod = request.method
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.doInput = true
            connection.doOutput = request.body.isNotEmpty()
            connection.setRequestProperty("Content-Type", request.contentType)
            connection.setRequestProperty("Accept", "application/json")
            request.headers.forEach(connection::setRequestProperty)
            if (request.body.isNotEmpty()) connection.outputStream.use { it.write(request.body) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            val headers = connection.headerFields.mapNotNull { (key, value) -> key?.let { it to value } }.toMap()
            return BackendResponse(status, body, headers)
        } catch (error: SocketTimeoutException) {
            throw BackendException(code = "timeout", message = "Backend request timed out.", cause = error)
        } catch (error: IOException) {
            throw BackendException(message = "Backend connection failed.", cause = error)
        }
    }
}

internal class EstudeNoahBackendClient(
    private val tokenSource: AppCheckTokenSource,
    private val transport: BackendTransport
) {
    suspend fun fromText(sourceType: String, sourceTitle: String, subject: String, grade: String, text: String): BackendGeneratedActivity {
        val body = JSONObject()
            .put("sourceType", sourceType)
            .put("sourceTitle", sourceTitle)
            .put("subject", subject)
            .put("grade", grade)
            .put("text", text)
        return parseActivity(executeJson("/v1/activities/from-text", body).body)
    }

    suspend fun fromPpt(fileName: String, bytes: ByteArray, subject: String, grade: String): BackendGeneratedActivity {
        val boundary = "EstudeNoah-${UUID.randomUUID()}"
        val crlf = "\r\n"
        val prefix = buildString {
            append("--$boundary$crlf")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            append(fileName.replace("\"", "_"))
            append("\"$crlf")
            append("Content-Type: application/vnd.ms-powerpoint$crlf$crlf")
        }.toByteArray(StandardCharsets.UTF_8)
        val suffix = buildString {
            append(crlf)
            append("--$boundary$crlf")
            append("Content-Disposition: form-data; name=\"subject\"$crlf$crlf$subject$crlf")
            append("--$boundary$crlf")
            append("Content-Disposition: form-data; name=\"grade\"$crlf$crlf$grade$crlf")
            append("--$boundary--$crlf")
        }.toByteArray(StandardCharsets.UTF_8)
        return parseActivity(execute(BackendRequest("POST", "/v1/activities/from-ppt", "multipart/form-data; boundary=$boundary", prefix + bytes + suffix)).body)
    }

    suspend fun fromYoutube(url: String, subject: String, grade: String): BackendGeneratedActivity {
        val analysisResponse = executeJson("/v1/materials/youtube/analyze", JSONObject().put("url", url))
        val analysis = parseObject(analysisResponse.body)
        val source = JSONObject()
            .put("type", "youtube")
            .put("title", analysis.optString("videoTitle", "Vídeo do YouTube"))
            .put("url", url)
        val request = JSONObject()
            .put("grade", grade)
            .put("subject", subject)
            .put("source", source)
            .put("analysis", analysis)
        return parseActivity(executeJson("/v1/activities/generate", request).body)
    }

    private suspend fun executeJson(path: String, json: JSONObject): BackendResponse = execute(
        BackendRequest("POST", path, "application/json; charset=utf-8", json.toString().toByteArray(StandardCharsets.UTF_8))
    )

    private suspend fun execute(request: BackendRequest): BackendResponse {
        var forceRefresh = false
        repeat(2) { attempt ->
            val token = try { tokenSource.token(forceRefresh) } catch (error: Exception) {
                throw BackendException(status = 401, code = "app_check_token_unavailable", message = "App Check unavailable.", cause = error)
            }
            val response = transport.execute(request.copy(headers = request.headers + (APP_CHECK_HEADER to token)))
            if (response.status in 200..299) return response
            val error = parseError(response)
            if (attempt == 0 && response.status == 401 && error.code == "app_check_token_invalid") {
                forceRefresh = true
            } else {
                throw error
            }
        }
        throw BackendException(status = 401, code = "app_check_token_invalid", message = "App Check rejected.")
    }

    private fun parseError(response: BackendResponse): BackendException {
        val json = runCatching { JSONObject(response.body) }.getOrNull()
        return BackendException(response.status, json?.optString("code")?.takeIf { it.isNotBlank() }, "Backend request failed with HTTP ${response.status}.")
    }

    private fun parseObject(raw: String): JSONObject = try {
        JSONObject(raw)
    } catch (error: Exception) {
        throw BackendException(code = "invalid_json", message = "Backend returned invalid JSON.", cause = error)
    }

    private fun parseActivity(raw: String): BackendGeneratedActivity {
        try {
            val root = JSONObject(raw)
            val themes = root.requireArray("themes").objects().map { theme ->
                BackendTheme(theme.requireString("name"), theme.requireArray("questions").objects().map { question ->
                    BackendQuestion(
                        statement = question.optNullableString("statement"),
                        answer = if (question.has("answer") && !question.isNull("answer")) question.getBoolean("answer") else null,
                        explanation = question.optString("explanation", ""),
                        problem = question.optNullableString("problem"),
                        mathAnswer = question.optNullableString("mathAnswer"),
                        solutionSteps = question.optJSONArray("solutionSteps")?.strings().orEmpty(),
                        difficulty = question.optNullableString("difficulty"),
                        theme = question.optNullableString("theme")
                    )
                })
            }
            if (themes.isEmpty() || themes.all { it.questions.isEmpty() }) throw IllegalArgumentException("No questions")
            return BackendGeneratedActivity(
                subject = root.requireString("subject"),
                grade = root.requireString("grade"),
                activityType = root.requireString("activityType"),
                themes = themes,
                warnings = root.optJSONArray("warnings")?.strings().orEmpty()
            )
        } catch (error: BackendException) {
            throw error
        } catch (error: Exception) {
            throw BackendException(code = "incompatible_response", message = "Backend response is incompatible.", cause = error)
        }
    }
}

private fun JSONObject.requireString(name: String): String = getString(name).also { require(it.isNotBlank()) }
private fun JSONObject.requireArray(name: String): JSONArray = getJSONArray(name)
private fun JSONObject.optNullableString(name: String): String? = if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

