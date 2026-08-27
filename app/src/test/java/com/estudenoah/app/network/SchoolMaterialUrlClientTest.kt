package com.estudenoah.app.network

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolMaterialUrlClientTest {
    @Test fun resolvesYoutubeAndSendsBearer() = runBlocking {
        val transport = FakeTransport(mutableListOf(okResolution()))
        val tokens = FakeTokens()
        val client = SchoolMaterialUrlClient(tokens, transport)

        val result = client.resolve("https://brasilescola.uol.com.br/videos/a.htm")

        assertEquals("youtube", result.kind)
        assertEquals("https://www.youtube.com/watch?v=ocjJ8bKEQ3Q", result.resolvedUrl)
        assertEquals("Brasil Colônia", result.title)
        assertEquals("Bearer secret-token", transport.requests.single().headers[AUTHORIZATION_HEADER])
        val body = JSONObject(transport.requests.single().body.toString(Charsets.UTF_8))
        assertEquals("https://brasilescola.uol.com.br/videos/a.htm", body.getString("url"))
    }

    @Test fun invalidTokenRetriesExactlyOnce() = runBlocking {
        val transport = FakeTransport(mutableListOf(error(401, "firebase_auth_token_invalid"), okResolution()))
        val tokens = FakeTokens()
        val client = SchoolMaterialUrlClient(tokens, transport)

        client.resolve("https://brasilescola.uol.com.br/videos/a.htm")

        assertEquals(listOf(false, true), tokens.calls)
        assertEquals(2, transport.requests.size)
    }

    @Test fun authenticatedAvaErrorGetsSpecificMessage() = runBlocking {
        val transport = FakeTransport(mutableListOf(error(422, "material_url_login_required")))
        val client = SchoolMaterialUrlClient(FakeTokens(), transport)

        val error = try {
            client.resolve("https://avarje.jesuitasbrasil.org.br/mod/url/view.php?id=1")
            throw AssertionError("Expected BackendException")
        } catch (failure: BackendException) {
            failure
        }

        assertTrue(error.userMessage().contains("depende do login do AVA"))
    }

    private class FakeTokens : AuthTokenSource {
        val calls = mutableListOf<Boolean>()
        override suspend fun token(forceRefresh: Boolean): String {
            calls += forceRefresh
            return if (forceRefresh) "fresh-token" else "secret-token"
        }
    }

    private class FakeTransport(private val responses: MutableList<BackendResponse>) : BackendTransport {
        val requests = mutableListOf<BackendRequest>()
        override suspend fun execute(request: BackendRequest): BackendResponse {
            requests += request
            return responses.removeAt(0)
        }
    }

    private fun okResolution() = BackendResponse(
        200,
        """{"kind":"youtube","inputUrl":"https://brasilescola.uol.com.br/videos/a.htm","resolvedUrl":"https://www.youtube.com/watch?v=ocjJ8bKEQ3Q","title":"Brasil Colônia"}"""
    )

    private fun error(status: Int, code: String) = BackendResponse(
        status,
        """{"code":"$code","message":"safe"}"""
    )
}
