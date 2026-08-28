package com.estudenoah.app.vieira

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeworkCompletionTest {
    private val math = LessonClass(2, "Matemática", "4º Ano", "Aula", "07:50", "08:40", null, null, "MATIFIC + livro p. 68")
    private val portuguese = LessonClass(1, "Português", "4º Ano", "Aula", "07:00", "07:50", null, null, "Leitura págs. 184 e 185")
    private val noHomework = LessonClass(3, "Inglês", "4º Ano", "Aula", "09:10", "10:00", "Vocabulary", "Food", null)
    private val date = "2026-08-28"

    @Test fun marksHomeworkAsCompletedManually() {
        val item = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L).single()
        assertTrue(item.completed)
        assertEquals(123L, item.completedAt)
        assertEquals(HomeworkCompletionSource.MANUAL, item.completionSource)
    }

    @Test fun undoKeepsTaskButClearsCompletionTimestamp() {
        val done = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        val undone = HomeworkCompletionHistory.set(done, date, math, false, 456L).single()
        assertFalse(undone.completed)
        assertNull(undone.completedAt)
    }

    @Test fun codecPersistsCompletionAcrossAppReopen() {
        val before = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        assertEquals(before, HomeworkCompletionJsonCodec.decode(HomeworkCompletionJsonCodec.encode(before)))
    }

    @Test fun reimportingSamePlanPreservesCompletionIdentity() {
        val original = DailyLessonPlan(date, listOf(math))
        val completion = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        val reimported = DailyLessonPlanHistory.upsert(listOf(original), original.copy()).single()
        assertEquals(1, HomeworkProgressCalculator.calculate(reimported, completion).completed)
    }

    @Test fun substantiallyChangedHomeworkDoesNotInheritCompletion() {
        val changed = math.copy(homework = "Nova tarefa: livro p. 90")
        assertNotEquals(HomeworkIdentity.key(date, math), HomeworkIdentity.key(date, changed))
        val completion = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        assertEquals(0, HomeworkProgressCalculator.calculate(DailyLessonPlan(date, listOf(changed)), completion).completed)
    }

    @Test fun differentTasksNeverShareState() {
        assertNotEquals(HomeworkIdentity.key(date, math), HomeworkIdentity.key(date, portuguese))
    }

    @Test fun progressCountsCompletedOverRealHomeworkOnly() {
        val plan = DailyLessonPlan(date, listOf(math, portuguese, noHomework))
        val completion = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        val progress = HomeworkProgressCalculator.calculate(plan, completion)
        assertEquals(1, progress.completed)
        assertEquals(2, progress.total)
        assertEquals("1 de 2 concluídas", progress.summary)
    }

    @Test fun completedHomeworkRemainsVisibleInPlan() {
        val plan = DailyLessonPlan(date, listOf(math))
        val completion = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L)
        assertTrue(completion.single().completed)
        assertEquals(math, plan.homeworkClasses.single())
        assertEquals("MATIFIC + livro p. 68", plan.homeworkClasses.single().homework)
    }

    @Test fun markingCompletionIsPureLocalStateAndRequiresNoPedagogicalPayload() {
        val completion = HomeworkCompletionHistory.set(emptyList(), date, math, true, 123L).single()
        val persisted = HomeworkCompletionJsonCodec.encode(listOf(completion))
        assertFalse(persisted.contains("MATIFIC"))
        assertFalse(persisted.contains("questions", ignoreCase = true))
        assertFalse(persisted.contains("gemini", ignoreCase = true))
    }
}
