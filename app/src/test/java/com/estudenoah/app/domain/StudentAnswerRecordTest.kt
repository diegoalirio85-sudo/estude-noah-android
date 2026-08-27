package com.estudenoah.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentAnswerRecordTest {
    @Test fun choiceSnapshotStoresSelectedAndCorrectAnswer() {
        val question = Question(
            id = "vf-1",
            prompt = "A Terra realiza movimento de translação ao redor do Sol.",
            options = listOf("Verdadeiro", "Falso"),
            correctIndex = 0,
            explanation = "A translação é o movimento da Terra ao redor do Sol."
        )

        val wrong = StudentAnswerRecord.fromChoice(question, 1)
        assertEquals("Falso", wrong.answer)
        assertEquals("Verdadeiro", wrong.correctAnswer)
        assertFalse(wrong.correct ?: true)
        assertEquals(question.prompt, wrong.prompt)

        val right = StudentAnswerRecord.fromChoice(question, 0)
        assertTrue(right.correct == true)
    }

    @Test fun mathSnapshotStoresExpectedAnswer() {
        val question = Question(
            id = "math-1",
            prompt = "Quanto é 12 ÷ 4?",
            options = emptyList(),
            correctIndex = 0,
            explanation = "12 ÷ 4 = 3.",
            mathAnswer = "3"
        )
        val record = StudentAnswerRecord.fromMath(question, " 2 ", false)
        assertEquals("2", record.answer)
        assertEquals("3", record.correctAnswer)
        assertEquals(question.prompt, record.prompt)
        assertEquals(false, record.correct)
    }
}
