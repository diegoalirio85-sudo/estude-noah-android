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
    val explanation: String
)

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
    val timestamp: Long
)

internal data class PreparedActivity(
    val id: String,
    val title: String,
    val subject: Subject,
    val sourceText: String,
    val questions: List<Question>,
    val createdAt: Long
)
