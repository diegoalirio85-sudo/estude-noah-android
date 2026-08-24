package com.estudenoah.app.network

import com.estudenoah.app.domain.Question
import com.estudenoah.app.domain.Subject
import java.util.UUID

internal object BackendActivityMapper {
    fun subject(label: String): Subject = when (label.trim().lowercase()) {
        "matemática", "matematica" -> Subject.MATEMATICA
        "ciências", "ciencias" -> Subject.CIENCIAS
        "história", "historia" -> Subject.HISTORIA
        "geografia" -> Subject.GEOGRAFIA
        else -> Subject.PORTUGUES
    }

    fun questions(activity: BackendGeneratedActivity): List<Question> = activity.themes.flatMap { theme ->
        theme.questions.map { item ->
            if (activity.activityType == "MATH_PROBLEMS") {
                val answer = item.mathAnswer ?: throw BackendException(code = "incompatible_response", message = "Math answer missing.")
                Question(
                    id = UUID.randomUUID().toString(),
                    prompt = item.problem ?: throw BackendException(code = "incompatible_response", message = "Math problem missing."),
                    options = listOf("Mostrar resposta"),
                    correctIndex = 0,
                    explanation = (listOf("Resposta: $answer") + item.solutionSteps + item.explanation).filter { it.isNotBlank() }.joinToString("\n")
                )
            } else {
                val answer = item.answer ?: throw BackendException(code = "incompatible_response", message = "True/false answer missing.")
                Question(
                    id = UUID.randomUUID().toString(),
                    prompt = item.statement ?: throw BackendException(code = "incompatible_response", message = "Statement missing."),
                    options = listOf("Verdadeiro", "Falso"),
                    correctIndex = if (answer) 0 else 1,
                    explanation = item.explanation
                )
            }
        }
    }
}

