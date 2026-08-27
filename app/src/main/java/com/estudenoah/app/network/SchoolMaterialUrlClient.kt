package com.estudenoah.app.network

import com.estudenoah.app.security.BackendLoginRequiredException
import java.nio.charset.StandardCharsets
import org.json.JSONObject

internal data class BackendResolvedMaterialUrl(
    val kind: String,
    val inputUrl: String,
    val resolvedUrl: String,
    val title: String
)

internal class SchoolMaterialUrlClient(
    private val tokenSource: AuthTokenSource = FirebaseTokenSource(),
    private val transport: BackendTransport = UrlConnectionBackendTransport()
) {
    suspend fun resolve(url: String): BackendResolvedMaterialUrl {
        val body = JSONObject().put("url", url).toString().toByteArray(StandardCharsets.UTF_8)
        val response = execute(
            BackendRequest(
                method = "POST",
                path = "/v1/materials/url/resolve",
                contentType = "application/json; charset=utf-8",
                body = body
            )
        )
        try {
            val json = JSONObject(response.body)
            val kind = json.getString("kind")
            val resolvedUrl = json.getString("resolvedUrl")
            require(kind == "youtube" && resolvedUrl.isNotBlank())
            return BackendResolvedMaterialUrl(
                kind = kind,
                inputUrl = json.optString("inputUrl", url),
                resolvedUrl = resolvedUrl,
                title = json.optString("title", "Material da escola").ifBlank { "Material da escola" }
            )
        } catch (error: BackendException) {
            throw error
        } catch (error: Exception) {
            throw BackendException(code = "incompatible_response", message = "Backend URL resolution response is incompatible.", cause = error)
        }
    }

    private suspend fun execute(request: BackendRequest): BackendResponse {
        var forceRefresh = false
        repeat(2) { attempt ->
            val token = try {
                tokenSource.token(forceRefresh)
            } catch (error: Exception) {
                val code = if (error is BackendLoginRequiredException) "firebase_login_required" else "firebase_auth_token_unavailable"
                throw BackendException(status = 401, code = code, message = "Firebase Authentication unavailable.", cause = error)
            }
            val response = transport.execute(
                request.copy(headers = request.headers + (AUTHORIZATION_HEADER to "Bearer $token"))
            )
            if (response.status in 200..299) return response

            val json = runCatching { JSONObject(response.body) }.getOrNull()
            val code = json?.optString("code")?.takeIf { it.isNotBlank() }
            if (attempt == 0 && response.status == 401 && code == "firebase_auth_token_invalid") {
                forceRefresh = true
            } else {
                throw BackendException(
                    status = response.status,
                    code = code,
                    message = "Backend URL resolution failed with HTTP ${response.status}."
                )
            }
        }
        throw BackendException(status = 401, code = "firebase_auth_token_invalid", message = "Firebase Authentication rejected.")
    }
}
