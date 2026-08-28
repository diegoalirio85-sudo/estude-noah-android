package com.estudenoah.app.vieira

import org.json.JSONArray
import org.json.JSONObject

internal object HomeworkCompletionJsonCodec {
    fun encode(items: List<HomeworkCompletion>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("date", item.date)
                item.subject?.let { put("subject", it) }
                item.lessonNumber?.let { put("lessonNumber", it) }
                put("lessonKey", item.lessonKey)
                put("homeworkKey", item.homeworkKey)
                put("completed", item.completed)
                item.completedAt?.let { put("completedAt", it) }
                put("completionSource", item.completionSource.name)
            })
        }
    }.toString()

    fun decode(raw: String): List<HomeworkCompletion> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val key = item.optString("homeworkKey").takeIf { it.isNotBlank() } ?: continue
                val date = item.optString("date").takeIf { it.isNotBlank() } ?: continue
                add(
                    HomeworkCompletion(
                        date = date,
                        subject = item.optString("subject").takeIf { it.isNotBlank() },
                        lessonNumber = item.optInt("lessonNumber", -1).takeIf { it >= 0 },
                        lessonKey = item.optString("lessonKey").takeIf { it.isNotBlank() } ?: "legacy:$key",
                        homeworkKey = key,
                        completed = item.optBoolean("completed", false),
                        completedAt = item.optLong("completedAt", -1L).takeIf { it >= 0L },
                        completionSource = runCatching {
                            HomeworkCompletionSource.valueOf(item.optString("completionSource"))
                        }.getOrDefault(HomeworkCompletionSource.MANUAL)
                    )
                )
            }
        }
    }.getOrElse { emptyList() }
}
