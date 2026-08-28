package com.estudenoah.app.vieira

internal data class DailyLessonPlan(
    val date: String,
    val classes: List<LessonClass>
) {
    val orderedClasses: List<LessonClass>
        get() = classes.sortedWith(compareBy<LessonClass> { it.startTime ?: "99:99" }.thenBy { it.lessonNumber ?: Int.MAX_VALUE })

    val homeworkClasses: List<LessonClass>
        get() = orderedClasses.filter { !it.homework.isNullOrBlank() }
}

internal data class LessonClass(
    val lessonNumber: Int?,
    val subject: String?,
    val classGroup: String?,
    val lessonType: String?,
    val startTime: String?,
    val endTime: String?,
    val plannedContent: String?,
    val completedContent: String?,
    val homework: String?
) {
    val displayContent: String?
        get() = completedContent?.takeIf { it.isNotBlank() } ?: plannedContent?.takeIf { it.isNotBlank() }
}

internal object DailyLessonPlanHistory {
    fun upsert(existing: List<DailyLessonPlan>, incoming: DailyLessonPlan): List<DailyLessonPlan> =
        (existing.filterNot { it.date == incoming.date } + incoming).sortedByDescending { it.date }
}
