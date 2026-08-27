package com.estudenoah.app.domain

internal enum class Subject(val label: String, val symbol: String) {
    PORTUGUES("Português", "Aa"),
    MATEMATICA("Matemática", "123"),
    CIENCIAS("Ciências", "✦"),
    HISTORIA("História", "⌛"),
    GEOGRAFIA("Geografia", "◎")
}

internal data class Question(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    var mathAnswer: String? = null,
    val solutionSteps: List<String> = emptyList()
) {
    init {
        mathAnswer = mathAnswer
            ?.trim()
            ?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    }

    val isMathProblem: Boolean get() = mathAnswer != null
}

internal data class CustomQuestion(
    val id: String,
    val subject: Subject,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
) {
    fun asQuestion() = Question(id, prompt, options, correctIndex, explanation)
}

internal data class HistoryEntry(
    val subject: String,
    val score: Int,
    val total: Int,
    val timestamp: Long,
    val answers: List<StudentAnswerRecord> = emptyList()
)

internal data class StudentAnswerRecord(
    val questionId: String,
    val answer: String,
    val correct: Boolean?,
    val prompt: String = "",
    val correctAnswer: String = "",
    val explanation: String = ""
) {
    companion object {
        fun fromChoice(question: Question, optionIndex: Int): StudentAnswerRecord {
            val selectedAnswer = question.options.getOrNull(optionIndex).orEmpty()
            val expectedAnswer = question.options.getOrNull(question.correctIndex).orEmpty()
            return StudentAnswerRecord(
                questionId = question.id,
                answer = selectedAnswer,
                correct = optionIndex == question.correctIndex,
                prompt = question.prompt,
                correctAnswer = expectedAnswer,
                explanation = question.explanation
            )
        }

        fun fromMath(question: Question, answer: String, correct: Boolean?): StudentAnswerRecord =
            StudentAnswerRecord(
                questionId = question.id,
                answer = answer.trim(),
                correct = correct,
                prompt = question.prompt,
                correctAnswer = question.mathAnswer.orEmpty(),
                explanation = question.explanation
            )
    }
}

internal data class PreparedActivity(
    val id: String,
    val title: String,
    val subject: Subject,
    val sourceText: String,
    val questions: List<Question>,
    val createdAt: Long
)
