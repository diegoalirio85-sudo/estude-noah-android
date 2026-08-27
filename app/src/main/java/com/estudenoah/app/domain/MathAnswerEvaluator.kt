package com.estudenoah.app.domain

import java.math.BigDecimal
import java.math.MathContext
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
        if (expectedNumber != null) {
            if (studentNumber != null) {
                return studentNumber.compareTo(expectedNumber) == 0
            }
            evaluateArithmeticAnswer(student, expectedNumber)?.let { return it }
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
        return parseDecimal(match.groupValues[1])
    }

    private fun evaluateArithmeticAnswer(value: String, expected: BigDecimal): Boolean? {
        val equation = Regex("^(.+?)\\s*=\\s*(.+)$").matchEntire(value)
        if (equation != null) {
            val left = parseBinaryExpression(equation.groupValues[1]) ?: return null
            val right = parseSimpleNumber(equation.groupValues[2]) ?: return null
            if (left.compareTo(right) != 0) return false
            return right.compareTo(expected) == 0
        }

        val result = parseBinaryExpression(value) ?: return null
        return result.compareTo(expected) == 0
    }

    private fun parseBinaryExpression(value: String): BigDecimal? {
        val match = Regex("^([+-]?\\d+(?:[.,]\\d+)?)\\s*([+\\-*/:x×÷])\\s*([+-]?\\d+(?:[.,]\\d+)?)$").matchEntire(value)
            ?: return null
        val left = parseDecimal(match.groupValues[1]) ?: return null
        val right = parseDecimal(match.groupValues[3]) ?: return null
        return when (match.groupValues[2]) {
            "+" -> left.add(right)
            "-" -> left.subtract(right)
            "*", "x", "×" -> left.multiply(right)
            "/", ":", "÷" -> if (right.compareTo(BigDecimal.ZERO) == 0) null else left.divide(right, MathContext.DECIMAL128)
            else -> null
        }
    }

    private fun parseDecimal(value: String): BigDecimal? =
        value.replace(',', '.').toBigDecimalOrNull()

    private fun looksComplex(value: String): Boolean = value.any { it in "/=×÷():*x" } ||
        value.count { it == ',' || it == '.' } > 1
}
