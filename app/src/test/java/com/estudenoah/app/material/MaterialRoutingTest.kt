package com.estudenoah.app.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialRoutingTest {
    @Test fun pptWithOfficialMimeUsesBackendAndSkipsLocalExtractor() = assertLegacy("aula.ppt", "application/vnd.ms-powerpoint")
    @Test fun uppercasePptUsesBackend() = assertLegacy("AULA.PPT", "application/vnd.ms-powerpoint")
    @Test fun pptWithGenericMimeUsesBackend() = assertLegacy(" aula final.ppt ", "application/octet-stream")
    @Test fun pptWithNullMimeUsesBackend() = assertLegacy("aula.ppt", null)
    @Test fun displayNameWinsWhenContentUriPathHasNoExtension() = assertLegacy("aula.ppt", null)

    @Test fun pptxStaysOnLocalExtractionEvenWithAmbiguousMime() {
        assertEquals(MaterialRoute.LOCAL_EXTRACTION, MaterialRouting.route("aula.pptx", "application/vnd.ms-powerpoint"))
    }

    @Test fun backendFailureNeverFallsBackToLocalExtraction() {
        var localCalled = false
        val failure = runCatching {
            MaterialRouting.dispatch(
                displayName = "aula.ppt",
                mimeType = null,
                legacyPpt = { error("backend failure") },
                localExtraction = { localCalled = true }
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertFalse(localCalled)
    }

    @Test fun officialLegacyMimeRoutesNamelessDocumentToBackend() {
        assertEquals(MaterialRoute.LEGACY_PPT_BACKEND, MaterialRouting.route("material", "application/vnd.ms-powerpoint"))
    }

    private fun assertLegacy(displayName: String, mimeType: String?) {
        var backendCalled = false
        var localCalled = false
        MaterialRouting.dispatch(
            displayName = displayName,
            mimeType = mimeType,
            legacyPpt = { backendCalled = true },
            localExtraction = { localCalled = true }
        )
        assertTrue(backendCalled)
        assertFalse(localCalled)
    }
}
