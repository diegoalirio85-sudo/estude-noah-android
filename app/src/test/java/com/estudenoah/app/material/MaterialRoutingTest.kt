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
    @Test fun ppsWithOfficialMimeUsesBackendAndSkipsLocalExtractor() = assertLegacy("aula.pps", "application/vnd.ms-powerpoint")
    @Test fun uppercasePpsUsesBackend() = assertLegacy("AULA.PPS", "application/vnd.ms-powerpoint")
    @Test fun ppsWithGenericMimeUsesBackend() = assertLegacy(" aula final.pps ", "application/octet-stream")
    @Test fun ppsWithNullMimeUsesBackend() = assertLegacy("aula.pps", null)
    @Test fun ppsDisplayNameWinsForContentUriWithoutExtension() = assertLegacy("aula.pps", null)
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

    @Test fun ppsBackendFailureNeverUsesLocalExtractorOrGenerator() {
        var localExtractorCalled = false
        var localGeneratorCalled = false
        runCatching {
            MaterialRouting.dispatch(
                displayName = "aula.pps",
                mimeType = "application/octet-stream",
                legacyPpt = { error("backend failure") },
                localExtraction = {
                    localExtractorCalled = true
                    localGeneratorCalled = true
                }
            )
        }
        assertFalse(localExtractorCalled)
        assertFalse(localGeneratorCalled)
    }

    @Test fun officialLegacyMimeRoutesNamelessDocumentToBackend() {
        assertEquals(MaterialRoute.LEGACY_PPT_BACKEND, MaterialRouting.route("material", "application/vnd.ms-powerpoint"))
    }

    @Test fun supportedLocalDocumentsGoDirectlyToTextPipeline() {
        listOf("aula.pdf", "aula.pptx", "aula.doc", "aula.docx", "aula.odt").forEach { name ->
            var textPipelineCalled = false
            MaterialRouting.dispatch(
                displayName = name,
                mimeType = "application/octet-stream",
                legacyPpt = { error("Legacy endpoint must not be used for $name") },
                localExtraction = { textPipelineCalled = true }
            )
            assertTrue("Text pipeline was not started for $name", textPipelineCalled)
        }
    }

    @Test fun intermediateContentIsNeverPartOfTheUiPolicy() {
        assertFalse(MaterialRouting.exposesIntermediateContent())
    }

    @Test fun extractedContentIsNotPersistedWithPreparedActivity() {
        assertEquals("", MaterialRouting.sourceTextForPersistence(false, "conteúdo extraído privado"))
        assertEquals("texto digitado", MaterialRouting.sourceTextForPersistence(true, "texto digitado"))
    }

    @Test fun processingFailureMessageIsFriendlyAndDoesNotExposeTechnicalContent() {
        assertTrue(MaterialRouting.FRIENDLY_PROCESSING_FAILURE.contains("Não foi possível processar"))
        assertFalse(MaterialRouting.FRIENDLY_PROCESSING_FAILURE.contains("extractedText"))
        assertFalse(MaterialRouting.FRIENDLY_PROCESSING_FAILURE.contains("stack", ignoreCase = true))
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
