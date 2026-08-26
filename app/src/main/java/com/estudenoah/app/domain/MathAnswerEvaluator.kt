package com.estudenoah.app.domain

import java.math.BigDecimal
import java.util.Locale

internal object MathAnswerEvaluator {
    const val INPUT_LABEL = "Sua resposta"
    const val SUBMIT_LABEL = "Responder"

    fun evaluate(studentAnswer: String, expectedAnswer: String): Boolean? {
        val student = normalizeText(studentAnswer)
        val expected = normalizeText(expectedAnswer)
        if (student.isBlank() || expected.isBlank()) return null
        if (student == expected) return true

        val studentNumber = parseSimpleNumber(student)
        val expectedNumber = parseSimpleNumber(expected)
        if (studentNumber != null && expectedNumber != null) {
            return studentNumber.compareTo(expectedNumber) == 0
        }

        return if (looksComplex(student) || looksComplex(expected)) null else false
    }

    fun shouldShowSolution(answered: Boolean): Boolean = answered

    fun prefersNumericKeyboard(expectedAnswer: String): Boolean =
        parseSimpleNumber(normalizeText(expectedAnswer)) != null

    private fun normalizeText(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .replace('−', '-')

    private fun parseSimpleNumber(value: String): BigDecimal? {
        val match = Regex("^(?:r\\$\\s*)?([+-]?\\d+(?:[.,]\\d+)?)(?:\\s*[a-zà-ÿ%²³]+)?$").matchEntire(value)
            ?: return null
        return match.groupValues[1].replace(',', '.').toBigDecimalOrNull()
    }

    private fun looksComplex(value: String): Boolean = value.any { it in "/=×÷()" } ||
        value.count { it == ',' || it == '.' } > 1
}
