package com.estudenoah.app.vieira

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLessonPlanTest {
    private fun fixture(): DailyLessonPlan {
        val raw = checkNotNull(javaClass.getResourceAsStream("/fixtures/vieira-plano-aula-2026-08-28.json"))
            .bufferedReader().use { it.readText() }
        return checkNotNull(DailyLessonPlanJsonCodec.decodePlan(raw))
    }

    @Test fun capturesFiveLessonsAndSelectedDate() {
        val plan = fixture()
        assertEquals("2026-08-28", plan.date)
        assertEquals(5, plan.classes.size)
    }

    @Test fun ordersClassesByStartTime() {
        assertEquals(
            listOf("Português", "Matemática", "Geografia", "Inglês", "Educação Física"),
            fixture().orderedClasses.map { it.subject }
        )
    }

    @Test fun preservesPlannedAndCompletedContentVerbatim() {
        val portuguese = fixture().classes.single { it.subject == "Português" }
        assertEquals("Leitura e interpretação de imagem", portuguese.plannedContent)
        assertEquals("LP - Leitura de imagem no livro págs. 184 e 185 (q. 1), 186", portuguese.completedContent)
    }

    @Test fun homeworkCreatesOnlyThreeRealActivities() {
        val homework = fixture().homeworkClasses
        assertEquals(listOf("Português", "Matemática", "Geografia"), homework.map { it.subject })
        assertFalse(homework.any { it.subject == "Inglês" })
        assertFalse(homework.any { it.subject == "Educação Física" })
    }

    @Test fun absentFieldsRemainNullAndNeverBecomeArtificialText() {
        val english = fixture().classes.single { it.subject == "Inglês" }
        assertNull(english.homework)
    }

    @Test fun reimportingSameDateReplacesWithoutDuplication() {
        val original = fixture()
        val changed = original.copy(classes = original.classes.map {
            if (it.subject == "Matemática") it.copy(completedContent = "Conteúdo atualizado") else it
        })
        val result = DailyLessonPlanHistory.upsert(listOf(original), changed)
        assertEquals(1, result.size)
        assertEquals("Conteúdo atualizado", result.single().classes.single { it.subject == "Matemática" }.completedContent)
    }

    @Test fun codecRoundTripKeepsStructuredHistory() {
        val decoded = DailyLessonPlanJsonCodec.decodePlans(DailyLessonPlanJsonCodec.encodePlans(listOf(fixture())))
        assertEquals(fixture(), decoded.single())
    }

    @Test fun extractionScriptNeverReadsOrTransmitsSessionSecrets() {
        val script = VieiraPlanDomExtractor.EXTRACTION_SCRIPT
        listOf("document.cookie", "localStorage", "sessionStorage", "authorization", "fetch(", "XMLHttpRequest")
            .forEach { forbidden -> assertFalse(forbidden, script.contains(forbidden, ignoreCase = true)) }
        assertTrue(script.contains("document.querySelectorAll"))
    }
}
