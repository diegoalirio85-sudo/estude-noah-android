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

        val studentValue = parseSimpleValue(student)
        val expectedValue = parseSimpleValue(expected)
        if (expectedValue != null) {
            if (studentValue != null) {
                return studentValue.number.compareTo(expectedValue.number) == 0 &&
                    unitsCompatible(studentValue.unit, expectedValue.unit)
            }
            evaluateArithmeticAnswer(student, expectedValue)?.let { return it }
        }

        return if (looksComplex(student) || looksComplex(expected)) null else false
    }

    fun shouldShowSolution(answered: Boolean): Boolean = answered

    fun prefersNumericKeyboard(expectedAnswer: String): Boolean =
        parseSimpleValue(normalizeText(expectedAnswer)) != null

    private data class SimpleValue(val number: BigDecimal, val unit: String?)

    private fun normalizeText(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .replace('−', '-')

    private fun parseSimpleValue(value: String): SimpleValue? {
        val match = Regex("^(?:(r\\$)\\s*)?([+-]?\\d+(?:[.,]\\d+)?)(?:\\s*([a-zà-ÿ%²³]+))?$").matchEntire(value)
            ?: return null
        val number = parseDecimal(match.groupValues[2]) ?: return null
        val prefixUnit = match.groupValues[1].takeIf { it.isNotBlank() }
        val suffixUnit = match.groupValues[3].takeIf { it.isNotBlank() }
        val unit = when {
            prefixUnit != null && suffixUnit != null -> {
                val prefix = canonicalUnit(prefixUnit)
                val suffix = canonicalUnit(suffixUnit)
                if (prefix != suffix) return null
                prefix
            }
            prefixUnit != null -> canonicalUnit(prefixUnit)
            suffixUnit != null -> canonicalUnit(suffixUnit)
            else -> null
        }
        return SimpleValue(number, unit)
    }

    private fun evaluateArithmeticAnswer(value: String, expected: SimpleValue): Boolean? {
        val equation = Regex("^(.+?)\\s*=\\s*(.+)$").matchEntire(value)
        if (equation != null) {
            val left = parseBinaryExpression(equation.groupValues[1]) ?: return null
            val right = parseSimpleValue(equation.groupValues[2]) ?: return null
            if (left.compareTo(right.number) != 0) return false
            return right.number.compareTo(expected.number) == 0 && unitsCompatible(right.unit, expected.unit)
        }

        val result = parseBinaryExpression(value) ?: return null
        return result.compareTo(expected.number) == 0
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

    private fun unitsCompatible(studentUnit: String?, expectedUnit: String?): Boolean =
        studentUnit == null || expectedUnit == null || studentUnit == expectedUnit

    private fun canonicalUnit(value: String): String = when (value.lowercase(Locale.ROOT)) {
        "r$", "real", "reais" -> "real"
        else -> value.lowercase(Locale.ROOT)
    }

    private fun parseDecimal(value: String): BigDecimal? =
        value.replace(',', '.').toBigDecimalOrNull()

    private fun looksComplex(value: String): Boolean = value.any { it in "/=×÷():*x" } ||
        value.count { it == ',' || it == '.' } > 1
}
