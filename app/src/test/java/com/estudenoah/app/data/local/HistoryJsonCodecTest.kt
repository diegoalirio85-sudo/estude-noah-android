package com.estudenoah.app.data.local

import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.StudentAnswerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryJsonCodecTest {
    @Test fun legacyHistoryWithoutAnswersRemainsCompatible() {
        val entries = HistoryJsonCodec.decode("""[{"subject":"Matemática","score":1,"total":2,"timestamp":10}]""")
        assertEquals(1, entries.size)
        assertTrue(entries.single().answers.isEmpty())
    }

    @Test fun studentMathAnswerRoundTrips() {
        val original = HistoryEntry("Matemática", 1, 2, 10, listOf(StudentAnswerRecord("q1", "24,5", true), StudentAnswerRecord("q2", "1/2", null)))
        val decoded = HistoryJsonCodec.decode(HistoryJsonCodec.encode(listOf(original))).single()
        assertEquals("24,5", decoded.answers[0].answer)
        assertEquals(true, decoded.answers[0].correct)
        assertNull(decoded.answers[1].correct)
    }
}
