package com.estudenoah.app.data.local

import android.content.Context
import com.estudenoah.app.domain.CustomQuestion
import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.PreparedActivity
import com.estudenoah.app.domain.Question
import com.estudenoah.app.domain.Subject
import org.json.JSONArray
import org.json.JSONObject

internal class LocalPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences(
        LocalPersistenceContract.PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun loadPreparedActivity(): PreparedActivity? {
        val raw = preferences.getString(
            LocalPersistenceContract.PREPARED_ACTIVITY_KEY,
            null
        ) ?: return null
        return runCatching {
            val item = JSONObject(raw)
            val subject = Subject.valueOf(item.getString("subject"))
            val questions = decodeQuestions(item.getJSONArray("questions").toString())
            if (questions.isEmpty()) return@runCatching null
            PreparedActivity(
                id = item.getString("id"),
                title = item.getString("title"),
                subject = subject,
                sourceText = item.optString("sourceText", ""),
                questions = questions,
                createdAt = item.optLong("createdAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun savePreparedActivity(activity: PreparedActivity) {
        val item = JSONObject()
            .put("id", activity.id)
            .put("title", activity.title)
            .put("subject", activity.subject.name)
            .put("sourceText", activity.sourceText)
            .put("questions", JSONArray(encodeQuestions(activity.questions)))
            .put("createdAt", activity.createdAt)
        preferences.edit()
            .putString(LocalPersistenceContract.PREPARED_ACTIVITY_KEY, item.toString())
            .apply()
    }

    fun clearPreparedActivity() {
        preferences.edit()
            .remove(LocalPersistenceContract.PREPARED_ACTIVITY_KEY)
            .apply()
    }

    fun loadCustomQuestions(): List<CustomQuestion> {
        val raw = preferences.getString(
            LocalPersistenceContract.CUSTOM_QUESTIONS_KEY,
            "[]"
        ) ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val subject = runCatching {
                        Subject.valueOf(item.getString("subject"))
                    }.getOrNull() ?: continue
                    val optionArray = item.getJSONArray("options")
                    val options = buildList {
                        for (j in 0 until optionArray.length()) {
                            add(optionArray.getString(j))
                        }
                    }
                    if (options.size == 4) {
                        add(
                            CustomQuestion(
                                id = item.getString("id"),
                                subject = subject,
                                prompt = item.getString("prompt"),
                                options = options,
                                correctIndex = item.getInt("correctIndex"),
                                explanation = item.optString("explanation", "")
                            )
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun saveCustomQuestions(questions: List<CustomQuestion>) {
        val array = JSONArray()
        questions.forEach { question ->
            array.put(
                JSONObject()
                    .put("id", question.id)
                    .put("subject", question.subject.name)
                    .put("prompt", question.prompt)
                    .put("options", JSONArray(question.options))
                    .put("correctIndex", question.correctIndex)
                    .put("explanation", question.explanation)
            )
        }
        preferences.edit()
            .putString(LocalPersistenceContract.CUSTOM_QUESTIONS_KEY, array.toString())
            .apply()
    }

    fun upsertCustomQuestion(question: CustomQuestion) {
        val current = loadCustomQuestions().toMutableList()
        val index = current.indexOfFirst { it.id == question.id }
        if (index >= 0) {
            current[index] = question
        } else {
            current.add(0, question)
        }
        saveCustomQuestions(current)
    }

    fun deleteCustomQuestion(id: String) {
        saveCustomQuestions(loadCustomQuestions().filterNot { it.id == id })
    }

    fun getParentPin(): String = preferences.getString(
        LocalPersistenceContract.PARENT_PIN_KEY,
        DEFAULT_PIN
    ) ?: DEFAULT_PIN

    fun setParentPin(pin: String) {
        preferences.edit()
            .putString(LocalPersistenceContract.PARENT_PIN_KEY, pin)
            .apply()
    }

    fun loadHistory(): List<HistoryEntry> {
        val raw = preferences.getString(
            LocalPersistenceContract.HISTORY_KEY,
            "[]"
        ) ?: "[]"
        return HistoryJsonCodec.decode(raw)
    }

    fun addHistory(entry: HistoryEntry) {
        val current = loadHistory().toMutableList()
        current.add(0, entry)
        preferences.edit()
            .putString(LocalPersistenceContract.HISTORY_KEY, HistoryJsonCodec.encode(current))
            .apply()
    }

    fun clearHistory() {
        preferences.edit()
            .remove(LocalPersistenceContract.HISTORY_KEY)
            .apply()
    }

    private fun encodeQuestions(questions: List<Question>): String {
        val array = JSONArray()
        questions.forEach { question ->
            array.put(
                JSONObject()
                    .put("id", question.id)
                    .put("prompt", question.prompt)
                    .put("options", JSONArray(question.options))
                    .put("correctIndex", question.correctIndex)
                    .put("explanation", question.explanation)
                    .put("mathAnswer", question.mathAnswer)
                    .put("solutionSteps", JSONArray(question.solutionSteps))
            )
        }
        return array.toString()
    }

    private fun decodeQuestions(raw: String): List<Question> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val optionArray = item.getJSONArray("options")
                val options = buildList {
                    for (j in 0 until optionArray.length()) {
                        add(optionArray.getString(j))
                    }
                }
                add(
                    Question(
                        id = item.optString("id", "session-$i"),
                        prompt = item.getString("prompt"),
                        options = options,
                        correctIndex = item.getInt("correctIndex"),
                        explanation = item.optString("explanation", ""),
                        mathAnswer = item.optString("mathAnswer", "").ifBlank { null },
                        solutionSteps = item.optJSONArray("solutionSteps")?.let { steps ->
                            buildList { for (j in 0 until steps.length()) add(steps.getString(j)) }
                        }.orEmpty()
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private companion object {
        const val DEFAULT_PIN = "1234"
    }
}
