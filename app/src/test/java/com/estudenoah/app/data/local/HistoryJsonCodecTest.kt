package com.estudenoah.app.data.local

import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.StudentAnswerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryJsonCodecTest {
    @Test fun legacyHistoryWithoutAnswersRemainsCompatible() {
        val entries = HistoryJsonCodec.decode("""[{"subject":"Matemática","score":1,"total":2,"timestamp":10}]""")
        assertEquals(1, entries.size)
        assertTrue(entries.single().answers.isEmpty())
    }

    @Test fun legacyAnswerWithoutSnapshotRemainsCompatible() {
        val raw = """[{"subject":"Matemática","score":0,"total":1,"timestamp":10,"answers":[{"questionId":"q1","answer":"8","correct":false}]}]"""
        val answer = HistoryJsonCodec.decode(raw).single().answers.single()
        assertEquals("8", answer.answer)
        assertFalse(answer.correct ?: true)
        assertEquals("", answer.prompt)
        assertEquals("", answer.correctAnswer)
        assertEquals("", answer.explanation)
    }

    @Test fun detailedAnswerSnapshotRoundTrips() {
        val original = HistoryEntry(
            "Ciências • Sistema Solar",
            0,
            1,
            10,
            listOf(
                StudentAnswerRecord(
                    questionId = "q1",
                    answer = "Verdadeiro",
                    correct = false,
                    prompt = "Netuno é o planeta mais próximo do Sol.",
                    correctAnswer = "Falso",
                    explanation = "Mercúrio é o planeta mais próximo do Sol."
                )
            )
        )
        val decoded = HistoryJsonCodec.decode(HistoryJsonCodec.encode(listOf(original))).single().answers.single()
        assertEquals("Netuno é o planeta mais próximo do Sol.", decoded.prompt)
        assertEquals("Verdadeiro", decoded.answer)
        assertEquals("Falso", decoded.correctAnswer)
        assertEquals(false, decoded.correct)
        assertEquals("Mercúrio é o planeta mais próximo do Sol.", decoded.explanation)
    }

    @Test fun nullableEvaluationStillRoundTrips() {
        val original = HistoryEntry("Matemática", 0, 1, 10, listOf(StudentAnswerRecord("q2", "1/2", null)))
        val decoded = HistoryJsonCodec.decode(HistoryJsonCodec.encode(listOf(original))).single()
        assertNull(decoded.answers.single().correct)
    }
}
