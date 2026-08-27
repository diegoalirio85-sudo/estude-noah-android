package com.estudenoah.app.data.local

import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.StudentAnswerRecord
import org.json.JSONArray
import org.json.JSONObject

internal object HistoryJsonCodec {
    fun decode(raw: String): List<HistoryEntry> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(HistoryEntry(
                    subject = item.getString("subject"),
                    score = item.getInt("score"),
                    total = item.getInt("total"),
                    timestamp = item.getLong("timestamp"),
                    answers = item.optJSONArray("answers")?.let(::decodeAnswers).orEmpty()
                ))
            }
        }.sortedByDescending { it.timestamp }
    }.getOrElse { emptyList() }

    fun encode(entries: List<HistoryEntry>): String = JSONArray().apply {
        entries.take(50).forEach { entry ->
            put(JSONObject()
                .put("subject", entry.subject)
                .put("score", entry.score)
                .put("total", entry.total)
                .put("timestamp", entry.timestamp)
                .put("answers", JSONArray().apply {
                    entry.answers.forEach { answer ->
                        put(JSONObject()
                            .put("questionId", answer.questionId)
                            .put("answer", answer.answer)
                            .put("correct", answer.correct))
                    }
                }))
        }
    }.toString()

    private fun decodeAnswers(array: JSONArray): List<StudentAnswerRecord> = buildList {
        for (i in 0 until array.length()) {
            val answer = array.getJSONObject(i)
            add(StudentAnswerRecord(
                questionId = answer.optString("questionId", ""),
                answer = answer.optString("answer", ""),
                correct = if (answer.has("correct") && !answer.isNull("correct")) answer.getBoolean("correct") else null
            ))
        }
    }
}
