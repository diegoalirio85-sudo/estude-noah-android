package com.estudenoah.app.vieira

import java.security.MessageDigest
import java.util.Locale

internal enum class HomeworkCompletionSource {
    MANUAL,
    APP_ACTIVITY
}

internal data class HomeworkCompletion(
    val date: String,
    val subject: String?,
    val lessonNumber: Int?,
    val lessonKey: String,
    val homeworkKey: String,
    val completed: Boolean,
    val completedAt: Long?,
    val completionSource: HomeworkCompletionSource
)

internal object HomeworkIdentity {
    fun lessonKey(date: String, lesson: LessonClass): String = hash(
        listOf(
            date,
            lesson.subject.orEmpty(),
            lesson.lessonNumber?.toString().orEmpty(),
            lesson.classGroup.orEmpty(),
            lesson.startTime.orEmpty()
        ).joinToString("|") { normalize(it) }
    )

    fun key(date: String, lesson: LessonClass): String {
        val stableContent = listOf(
            date,
            lesson.subject.orEmpty(),
            lesson.lessonNumber?.toString().orEmpty(),
            lesson.classGroup.orEmpty(),
            lesson.startTime.orEmpty(),
            lesson.homework.orEmpty()
        ).joinToString("|") { normalize(it) }
        return hash(stableContent)
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
}

internal object HomeworkCompletionHistory {
    fun set(
        existing: List<HomeworkCompletion>,
        date: String,
        lesson: LessonClass,
        completed: Boolean,
        now: Long,
        source: HomeworkCompletionSource = HomeworkCompletionSource.MANUAL
    ): List<HomeworkCompletion> {
        val key = HomeworkIdentity.key(date, lesson)
        val previous = existing.firstOrNull { it.homeworkKey == key }
        val updated = HomeworkCompletion(
            date = date,
            subject = lesson.subject,
            lessonNumber = lesson.lessonNumber,
            lessonKey = HomeworkIdentity.lessonKey(date, lesson),
            homeworkKey = key,
            completed = completed,
            completedAt = when {
                !completed -> null
                previous?.completed == true -> previous.completedAt
                else -> now
            },
            completionSource = source
        )
        return existing.filterNot { it.homeworkKey == key } + updated
    }
}

internal object HomeworkCompletionUi {
    const val PENDING_LABEL = "[ ] Marcar como feita"
    const val COMPLETED_LABEL = "[✓] Concluída"
    const val UNDO_LABEL = "Desfazer"
    const val EMPTY_LABEL = "Não há atividades de casa registradas para hoje."
}

internal data class HomeworkProgress(val completed: Int, val total: Int) {
    val summary: String get() = "$completed de $total concluídas"
}

internal object HomeworkProgressCalculator {
    fun calculate(plan: DailyLessonPlan, completions: List<HomeworkCompletion>): HomeworkProgress {
        val completedKeys = completions.filter { it.completed }.map { it.homeworkKey }.toSet()
        return HomeworkProgress(
            completed = plan.homeworkClasses.count { HomeworkIdentity.key(plan.date, it) in completedKeys },
            total = plan.homeworkClasses.size
        )
    }
}
