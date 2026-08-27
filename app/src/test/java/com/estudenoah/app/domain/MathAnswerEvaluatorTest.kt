package com.estudenoah.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathAnswerEvaluatorTest {
    @Test fun mathProblemsExposeAnswerFieldPolicy() { assertEquals("Sua resposta", MathAnswerEvaluator.INPUT_LABEL); assertEquals("Responder", MathAnswerEvaluator.SUBMIT_LABEL) }
    @Test fun correctIntegerIsRecognized() { assertEquals(true, MathAnswerEvaluator.evaluate("24", "24")) }
    @Test fun wrongIntegerIsRecognized() { assertEquals(false, MathAnswerEvaluator.evaluate("23", "24")) }
    @Test fun numericFormattingIsEquivalent() { assertEquals(true, MathAnswerEvaluator.evaluate(" 24 ", "24")) }
    @Test fun commaAndPointDecimalsAreEquivalent() { assertEquals(true, MathAnswerEvaluator.evaluate("24,50", "24.5")) }
    @Test fun monetaryAndSimpleUnitAnswersAreAcceptedNumerically() { assertEquals(true, MathAnswerEvaluator.evaluate("R$ 24,50", "24.5 reais")) }
    @Test fun verifiedDivisionEquationIsEquivalentToExpectedUnitAnswer() { assertEquals(true, MathAnswerEvaluator.evaluate("12:4=3", "3 lápis")) }
    @Test fun verifiedDivisionExpressionIsEquivalentToExpectedUnitAnswer() { assertEquals(true, MathAnswerEvaluator.evaluate("12÷4", "3 lápis")); assertEquals(true, MathAnswerEvaluator.evaluate("12/4=3", "3 lápis")) }
    @Test fun incorrectEquationResultIsRejected() { assertEquals(false, MathAnswerEvaluator.evaluate("12:4=4", "3 lápis")) }
    @Test fun arithmeticResultDifferentFromExpectedIsRejected() { assertEquals(false, MathAnswerEvaluator.evaluate("12:3=4", "3 lápis")) }
    @Test fun numericExpectedAnswerPrefersDecimalKeyboard() { assertTrue(MathAnswerEvaluator.prefersNumericKeyboard("-24,5")); assertFalse(MathAnswerEvaluator.prefersNumericKeyboard("um meio")) }
    @Test fun unsupportedComplexAnswersAreNotGuessed() { assertNull(MathAnswerEvaluator.evaluate("(2+2)*3", "12")); assertNull(MathAnswerEvaluator.evaluate("2/4", "1/2")) }
    @Test fun divisionByZeroIsNotGuessed() { assertNull(MathAnswerEvaluator.evaluate("12:0", "3")) }
    @Test fun solutionIsHiddenBeforeAttempt() { assertFalse(MathAnswerEvaluator.shouldShowSolution(false)); assertTrue(MathAnswerEvaluator.shouldShowSolution(true)) }
    @Test fun historyModelRemainsCompatibleAndRecordsStudentAnswer() { val legacy=HistoryEntry("Matemática",1,1,1L); assertTrue(legacy.answers.isEmpty()); val recorded=legacy.copy(answers=listOf(StudentAnswerRecord("q1","24",true))); assertEquals("24",recorded.answers.single().answer); assertEquals(true,recorded.answers.single().correct) }
    @Test fun trueFalseQuestionRemainsNonMath() { val question=Question("q","A água ferve.",listOf("Verdadeiro","Falso"),0,"Explicação"); assertFalse(question.isMathProblem); assertEquals(listOf("Verdadeiro","Falso"),question.options) }
    @Test fun serializedNullIsSanitizedAtModelBoundary() { val question=Question("q","A água ferve.",listOf("Verdadeiro","Falso"),0,"Explicação",mathAnswer=" null "); assertNull(question.mathAnswer); assertFalse(question.isMathProblem) }
    @Test fun blankMathAnswerIsSanitizedAtModelBoundary() { val question=Question("q","A água ferve.",listOf("Verdadeiro","Falso"),0,"Explicação",mathAnswer="   "); assertNull(question.mathAnswer); assertFalse(question.isMathProblem) }
}
