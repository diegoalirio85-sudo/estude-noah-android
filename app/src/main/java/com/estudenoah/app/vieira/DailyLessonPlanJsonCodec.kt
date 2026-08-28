package com.estudenoah.app.vieira

import org.json.JSONArray
import org.json.JSONObject

internal object DailyLessonPlanJsonCodec {
    fun decodePlan(raw: String): DailyLessonPlan? = runCatching {
        val root = JSONObject(raw)
        val date = root.optString("date").trim()
        val array = root.optJSONArray("classes") ?: return@runCatching null
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return@runCatching null
        val classes = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    LessonClass(
                        lessonNumber = item.optInt("lessonNumber", -1).takeIf { it >= 0 },
                        subject = item.textOrNull("subject"),
                        classGroup = item.textOrNull("classGroup"),
                        lessonType = item.textOrNull("lessonType"),
                        startTime = item.textOrNull("startTime"),
                        endTime = item.textOrNull("endTime"),
                        plannedContent = item.textOrNull("plannedContent"),
                        completedContent = item.textOrNull("completedContent"),
                        homework = item.textOrNull("homework")
                    )
                )
            }
        }
        DailyLessonPlan(date, classes)
    }.getOrNull()

    fun encodePlans(plans: List<DailyLessonPlan>): String = JSONArray().apply {
        plans.forEach { put(encodePlan(it)) }
    }.toString()

    fun decodePlans(raw: String): List<DailyLessonPlan> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                decodePlan(array.getJSONObject(index).toString())?.let(::add)
            }
        }
    }.getOrElse { emptyList() }

    private fun encodePlan(plan: DailyLessonPlan): JSONObject = JSONObject()
        .put("date", plan.date)
        .put("classes", JSONArray().apply {
            plan.classes.forEach { lesson ->
                put(JSONObject().apply {
                    lesson.lessonNumber?.let { put("lessonNumber", it) }
                    putNullable("subject", lesson.subject)
                    putNullable("classGroup", lesson.classGroup)
                    putNullable("lessonType", lesson.lessonType)
                    putNullable("startTime", lesson.startTime)
                    putNullable("endTime", lesson.endTime)
                    putNullable("plannedContent", lesson.plannedContent)
                    putNullable("completedContent", lesson.completedContent)
                    putNullable("homework", lesson.homework)
                })
            }
        })

    private fun JSONObject.textOrNull(name: String): String? =
        optString(name).trim().takeIf { it.isNotEmpty() && it != "null" }

    private fun JSONObject.putNullable(name: String, value: String?) {
        if (!value.isNullOrBlank()) put(name, value)
    }
}
