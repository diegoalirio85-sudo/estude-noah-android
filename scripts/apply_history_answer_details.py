from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(relative_path: str, old: str, new: str) -> None:
    path = ROOT / relative_path
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {relative_path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/estudenoah/app/domain/DomainModels.kt",
    '''internal data class StudentAnswerRecord(
    val questionId: String,
    val answer: String,
    val correct: Boolean?
)
''',
    '''internal data class StudentAnswerRecord(
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
'''
)

replace_once(
    "app/src/main/java/com/estudenoah/app/data/local/HistoryJsonCodec.kt",
    '''                        put(JSONObject()
                            .put("questionId", answer.questionId)
                            .put("answer", answer.answer)
                            .put("correct", answer.correct))
''',
    '''                        put(JSONObject()
                            .put("questionId", answer.questionId)
                            .put("answer", answer.answer)
                            .put("correct", answer.correct)
                            .put("prompt", answer.prompt)
                            .put("correctAnswer", answer.correctAnswer)
                            .put("explanation", answer.explanation))
'''
)

replace_once(
    "app/src/main/java/com/estudenoah/app/data/local/HistoryJsonCodec.kt",
    '''            add(StudentAnswerRecord(
                questionId = answer.optString("questionId", ""),
                answer = answer.optString("answer", ""),
                correct = if (answer.has("correct") && !answer.isNull("correct")) answer.getBoolean("correct") else null
            ))
''',
    '''            add(StudentAnswerRecord(
                questionId = answer.optString("questionId", ""),
                answer = answer.optString("answer", ""),
                correct = if (answer.has("correct") && !answer.isNull("correct")) answer.getBoolean("correct") else null,
                prompt = answer.optString("prompt", ""),
                correctAnswer = answer.optString("correctAnswer", ""),
                explanation = answer.optString("explanation", "")
            ))
'''
)

replace_once(
    "app/src/main/java/com/estudenoah/app/MainActivity.kt",
    '''                        onAnswer = { optionIndex ->
                            if (!solved) {
                                if (optionIndex == question.correctIndex) {
                                    if (!firstAttemptAlreadyUsed) score += 1
                                    solved = true
                                    feedback = "Muito bem! Resposta correta."
                                } else {
                                    firstAttemptAlreadyUsed = true
                                    feedback = "Ainda não. Tente novamente."
                                }
                            }
                        },
''',
    '''                        onAnswer = { optionIndex ->
                            if (!solved) {
                                if (studentAnswers.none { it.questionId == question.id }) {
                                    studentAnswers = studentAnswers + StudentAnswerRecord.fromChoice(question, optionIndex)
                                }
                                if (optionIndex == question.correctIndex) {
                                    if (!firstAttemptAlreadyUsed) score += 1
                                    solved = true
                                    feedback = "Muito bem! Resposta correta."
                                } else {
                                    firstAttemptAlreadyUsed = true
                                    feedback = "Ainda não. Tente novamente."
                                }
                            }
                        },
'''
)

replace_once(
    "app/src/main/java/com/estudenoah/app/MainActivity.kt",
    '''                                studentAnswers = studentAnswers + StudentAnswerRecord(question.id, studentAnswer.trim(), evaluation)
''',
    '''                                studentAnswers = studentAnswers + StudentAnswerRecord.fromMath(question, studentAnswer, evaluation)
'''
)

replace_once(
    "app/src/main/java/com/estudenoah/app/MainActivity.kt",
    '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(entries: List<HistoryEntry>, onBack: () -> Unit, onClear: () -> Unit) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                actions = { if (entries.isNotEmpty()) TextButton(onClick = onClear) { Text("Limpar", color = Red) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ainda não há atividades concluídas.", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Quando Noah terminar uma atividade, o resultado aparecerá aqui.", color = Muted, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(entries) { entry -> HistoryCard(entry) }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    Card(modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(58.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Text("${entry.score}/${entry.total}", color = Blue, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.subject, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(formatter.format(Date(entry.timestamp)), color = Muted)
            }
            val percentage = if (entry.total == 0) 0 else entry.score * 100 / entry.total
            Text("$percentage%", color = Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}
''',
    '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(entries: List<HistoryEntry>, onBack: () -> Unit, onClear: () -> Unit) {
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }
    val detailEntry = selectedEntry
    if (detailEntry != null) {
        BackHandler { selectedEntry = null }
        HistoryDetailScreen(entry = detailEntry, onBack = { selectedEntry = null })
        return
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                actions = { if (entries.isNotEmpty()) TextButton(onClick = onClear) { Text("Limpar", color = Red) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ainda não há atividades concluídas.", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Quando Noah terminar uma atividade, o resultado aparecerá aqui.", color = Muted, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(entries) { entry -> HistoryCard(entry, onClick = { selectedEntry = entry }) }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry, onClick: () -> Unit) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(58.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Text("${entry.score}/${entry.total}", color = Blue, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.subject, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(formatter.format(Date(entry.timestamp)), color = Muted)
                Text("Toque para ver as respostas", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            val percentage = if (entry.total == 0) 0 else entry.score * 100 / entry.total
            Text("$percentage%", color = Green, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailScreen(entry: HistoryEntry, onBack: () -> Unit) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val detailedAnswers = entry.answers.filter { it.prompt.isNotBlank() }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da atividade", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Histórico") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueSoft),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(entry.subject, color = Blue, fontWeight = FontWeight.Bold)
                        Text("${entry.score} de ${entry.total} acertos de primeira", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(formatter.format(Date(entry.timestamp)), color = Muted)
                        Spacer(Modifier.height(6.dp))
                        Text("A pontuação e este histórico consideram sempre o primeiro palpite.", color = Muted, fontSize = 13.sp)
                    }
                }
            }

            if (detailedAnswers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Os detalhes das respostas não estão disponíveis para esta atividade porque ela foi concluída antes desta atualização. O resultado geral foi preservado.",
                            modifier = Modifier.padding(20.dp),
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(detailedAnswers.size) { index ->
                    HistoryAnswerCard(number = index + 1, answer = detailedAnswers[index])
                }
                if (detailedAnswers.size < entry.total) {
                    item {
                        Text(
                            "Algumas respostas desta atividade foram registradas por uma versão anterior e não possuem detalhes completos.",
                            modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp),
                            color = Muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAnswerCard(number: Int, answer: StudentAnswerRecord) {
    val trueFalse = answer.correctAnswer == "Verdadeiro" || answer.correctAnswer == "Falso"
    val containerColor = when (answer.correct) {
        true -> GreenSoft
        false -> RedSoft
        null -> BlueSoft
    }
    val statusColor = when (answer.correct) {
        true -> Green
        false -> Red
        null -> Blue
    }
    val status = when (answer.correct) {
        true -> "✓ Acertou de primeira"
        false -> "✕ Errou de primeira"
        null -> "Resposta registrada"
    }

    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(if (trueFalse) "Afirmação $number" else "Questão $number", color = statusColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(answer.prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(status, color = statusColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text("Sua resposta: ${answer.answer.ifBlank { "Não registrada" }}", color = Ink)
            if (answer.correct != true && answer.correctAnswer.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text("Resposta correta: ${answer.correctAnswer}", color = Ink, fontWeight = FontWeight.Bold)
            }
            if (answer.correct == false && answer.explanation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(answer.explanation, color = Ink)
            }
        }
    }
}
'''
)

(ROOT / "app/src/test/java/com/estudenoah/app/data/local/HistoryJsonCodecTest.kt").write_text('''package com.estudenoah.app.data.local

import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.StudentAnswerRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryJsonCodecTest {
    @Test fun legacyHistoryWithoutAnswersRemainsCompatible() {
        val entries = HistoryJsonCodec.decode("""[{"subject":"Matemática","score":1,"total":2,"timestamp":10}]""")
        assertEquals(1, entries.size)
        assertTrue(entries.single().answers.isEmpty())
    }

    @Test fun legacyAnswerWithoutSnapshotRemainsCompatible() {
        val raw = """[{"subject":"Matemática","score":0,"total":1,"timestamp":10,"answers":[{"questionId":"q1","answer":"8","correct":false}]}]"""
        val answer = HistoryJsonCodec.decode(raw).single().answers.single()
        assertEquals("8", answer.answer)
        assertFalse(answer.correct ?: true)
        assertEquals("", answer.prompt)
        assertEquals("", answer.correctAnswer)
        assertEquals("", answer.explanation)
    }

    @Test fun detailedAnswerSnapshotRoundTrips() {
        val original = HistoryEntry(
            "Ciências • Sistema Solar",
            0,
            1,
            10,
            listOf(
                StudentAnswerRecord(
                    questionId = "q1",
                    answer = "Verdadeiro",
                    correct = false,
                    prompt = "Netuno é o planeta mais próximo do Sol.",
                    correctAnswer = "Falso",
                    explanation = "Mercúrio é o planeta mais próximo do Sol."
                )
            )
        )
        val decoded = HistoryJsonCodec.decode(HistoryJsonCodec.encode(listOf(original))).single().answers.single()
        assertEquals("Netuno é o planeta mais próximo do Sol.", decoded.prompt)
        assertEquals("Verdadeiro", decoded.answer)
        assertEquals("Falso", decoded.correctAnswer)
        assertEquals(false, decoded.correct)
        assertEquals("Mercúrio é o planeta mais próximo do Sol.", decoded.explanation)
    }

    @Test fun nullableEvaluationStillRoundTrips() {
        val original = HistoryEntry("Matemática", 0, 1, 10, listOf(StudentAnswerRecord("q2", "1/2", null)))
        val decoded = HistoryJsonCodec.decode(HistoryJsonCodec.encode(listOf(original))).single()
        assertNull(decoded.answers.single().correct)
    }
}
''', encoding="utf-8")

(ROOT / "app/src/test/java/com/estudenoah/app/domain/StudentAnswerRecordTest.kt").write_text('''package com.estudenoah.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentAnswerRecordTest {
    @Test fun choiceSnapshotStoresSelectedAndCorrectAnswer() {
        val question = Question(
            id = "vf-1",
            prompt = "A Terra realiza movimento de translação ao redor do Sol.",
            options = listOf("Verdadeiro", "Falso"),
            correctIndex = 0,
            explanation = "A translação é o movimento da Terra ao redor do Sol."
        )

        val wrong = StudentAnswerRecord.fromChoice(question, 1)
        assertEquals("Falso", wrong.answer)
        assertEquals("Verdadeiro", wrong.correctAnswer)
        assertFalse(wrong.correct ?: true)
        assertEquals(question.prompt, wrong.prompt)

        val right = StudentAnswerRecord.fromChoice(question, 0)
        assertTrue(right.correct == true)
    }

    @Test fun mathSnapshotStoresExpectedAnswer() {
        val question = Question(
            id = "math-1",
            prompt = "Quanto é 12 ÷ 4?",
            options = emptyList(),
            correctIndex = 0,
            explanation = "12 ÷ 4 = 3.",
            mathAnswer = "3"
        )
        val record = StudentAnswerRecord.fromMath(question, " 2 ", false)
        assertEquals("2", record.answer)
        assertEquals("3", record.correctAnswer)
        assertEquals(question.prompt, record.prompt)
        assertEquals(false, record.correct)
    }
}
''', encoding="utf-8")

print("History answer detail patch applied successfully")
