package com.estudenoah.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EstudeNoahTheme {
                EstudeNoahApp()
            }
        }
    }
}

private val Cream = Color(0xFFFFF8F0)
private val Blue = Color(0xFF3F63C8)
private val BlueSoft = Color(0xFFE9EEFF)
private val Green = Color(0xFF2E7D5B)
private val GreenSoft = Color(0xFFE7F6EE)
private val Red = Color(0xFFB54848)
private val RedSoft = Color(0xFFFFEAEA)
private val Yellow = Color(0xFFFFD166)
private val Ink = Color(0xFF243047)
private val Muted = Color(0xFF65708A)

@Composable
private fun EstudeNoahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            background = Cream,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        ),
        content = content
    )
}

private enum class AppScreen { HOME, SUBJECTS, QUIZ, RESULT, HISTORY }

private enum class Subject(val label: String, val symbol: String) {
    PORTUGUES("Português", "Aa"),
    MATEMATICA("Matemática", "123"),
    CIENCIAS("Ciências", "✦"),
    HISTORIA("História", "⌛"),
    GEOGRAFIA("Geografia", "◎")
}

private data class Question(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private data class HistoryEntry(
    val subject: String,
    val score: Int,
    val total: Int,
    val timestamp: Long
)

private object QuestionBank {
    private val questions = mapOf(
        Subject.PORTUGUES to listOf(
            Question(
                "Qual palavra está escrita corretamente?",
                listOf("Caza", "Casa", "Cassa", "Kasa"),
                1,
                "Casa é escrita com S entre as vogais."
            ),
            Question(
                "Na frase “O menino correu rápido”, qual palavra indica uma ação?",
                listOf("menino", "rápido", "correu", "o"),
                2,
                "“Correu” é o verbo da frase e indica a ação praticada pelo menino."
            ),
            Question(
                "Qual é o plural de “animal”?",
                listOf("animals", "animales", "animais", "animãos"),
                2,
                "O plural correto de animal é animais."
            ),
            Question(
                "Qual sinal usamos normalmente ao final de uma pergunta?",
                listOf("!", ".", ",", "?"),
                3,
                "O ponto de interrogação (?) indica uma pergunta."
            ),
            Question(
                "Em “A bola azul caiu”, qual palavra caracteriza a bola?",
                listOf("azul", "caiu", "a", "bola"),
                0,
                "“Azul” informa uma característica da bola."
            )
        ),
        Subject.MATEMATICA to listOf(
            Question(
                "Quanto é 8 + 7?",
                listOf("13", "14", "15", "16"),
                2,
                "8 + 7 = 15."
            ),
            Question(
                "Quanto é 6 × 4?",
                listOf("10", "20", "24", "28"),
                2,
                "6 grupos de 4 formam 24."
            ),
            Question(
                "Qual número vem imediatamente antes de 100?",
                listOf("98", "99", "101", "90"),
                1,
                "O número imediatamente anterior a 100 é 99."
            ),
            Question(
                "Uma dúzia corresponde a quantas unidades?",
                listOf("6", "10", "12", "20"),
                2,
                "Uma dúzia é um conjunto de 12 unidades."
            ),
            Question(
                "Se você tinha 20 figurinhas e deu 5, com quantas ficou?",
                listOf("10", "15", "20", "25"),
                1,
                "20 − 5 = 15."
            )
        ),
        Subject.CIENCIAS to listOf(
            Question(
                "Qual destes animais é um mamífero?",
                listOf("Galinha", "Cachorro", "Tartaruga", "Sardinha"),
                1,
                "O cachorro é um mamífero: nasce do corpo da mãe e, quando filhote, mama."
            ),
            Question(
                "Qual parte da planta normalmente absorve água do solo?",
                listOf("Raiz", "Flor", "Fruto", "Folha"),
                0,
                "As raízes fixam a planta e absorvem água e sais minerais do solo."
            ),
            Question(
                "Qual destes elementos é essencial para respirarmos?",
                listOf("Areia", "Oxigênio", "Plástico", "Vidro"),
                1,
                "Nosso corpo utiliza o oxigênio presente no ar durante a respiração."
            ),
            Question(
                "Em qual estado físico a água está quando vira gelo?",
                listOf("Gasoso", "Líquido", "Sólido", "Vapor"),
                2,
                "O gelo é água no estado sólido."
            ),
            Question(
                "Qual astro ilumina naturalmente a Terra durante o dia?",
                listOf("Lua", "Marte", "Sol", "Saturno"),
                2,
                "O Sol é a estrela que fornece luz e calor à Terra."
            )
        ),
        Subject.HISTORIA to listOf(
            Question(
                "Para estudar o passado, os historiadores utilizam principalmente:",
                listOf("fontes históricas", "previsões do tempo", "apenas mapas", "somente números"),
                0,
                "Fontes históricas podem ser documentos, objetos, imagens, relatos e muitos outros vestígios."
            ),
            Question(
                "Uma fotografia antiga pode ser considerada:",
                listOf("um brinquedo", "uma fonte histórica", "um planeta", "uma operação matemática"),
                1,
                "Fotografias registram pessoas, lugares e acontecimentos de uma época."
            ),
            Question(
                "Quando organizamos acontecimentos do mais antigo para o mais recente, fazemos uma:",
                listOf("receita", "linha do tempo", "tabuada", "legenda"),
                1,
                "A linha do tempo ajuda a visualizar a ordem cronológica dos acontecimentos."
            ),
            Question(
                "Os relatos de pessoas que viveram um acontecimento são exemplos de:",
                listOf("fontes orais", "fontes minerais", "formas geométricas", "medidas de massa"),
                0,
                "Depoimentos e entrevistas são fontes orais."
            ),
            Question(
                "Estudar a história de uma família ajuda a compreender:",
                listOf("apenas o futuro", "mudanças e permanências ao longo do tempo", "somente contas", "apenas animais"),
                1,
                "A história permite observar o que mudou e o que permaneceu ao longo do tempo."
            )
        ),
        Subject.GEOGRAFIA to listOf(
            Question(
                "Qual representação mostra a superfície de um lugar vista de cima?",
                listOf("Mapa", "Poema", "Receita", "Canção"),
                0,
                "Mapas representam espaços e ajudam a localizar lugares."
            ),
            Question(
                "Em qual planeta nós vivemos?",
                listOf("Marte", "Terra", "Vênus", "Júpiter"),
                1,
                "Vivemos no planeta Terra."
            ),
            Question(
                "Uma área com muitos prédios, ruas e comércio é geralmente uma paisagem:",
                listOf("urbana", "marinha", "desértica", "polar"),
                0,
                "Paisagens urbanas são marcadas pela concentração de construções e atividades da cidade."
            ),
            Question(
                "Qual ponto cardeal é indicado pela letra N?",
                listOf("Sul", "Leste", "Oeste", "Norte"),
                3,
                "A letra N representa o Norte."
            ),
            Question(
                "Rios, montanhas e vegetação são exemplos de elementos:",
                listOf("naturais", "digitais", "musicais", "numéricos"),
                0,
                "Esses elementos fazem parte da natureza e compõem as paisagens."
            )
        )
    )

    fun get(subject: Subject): List<Question> = questions[subject].orEmpty()
}

private object HistoryStorage {
    private const val PREFS = "estude_noah_prefs"
    private const val KEY_HISTORY = "history"

    fun load(context: Context): List<HistoryEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]") ?: "[]"

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        HistoryEntry(
                            subject = item.getString("subject"),
                            score = item.getInt("score"),
                            total = item.getInt("total"),
                            timestamp = item.getLong("timestamp")
                        )
                    )
                }
            }.sortedByDescending { it.timestamp }
        }.getOrElse { emptyList() }
    }

    fun add(context: Context, entry: HistoryEntry) {
        val current = load(context).toMutableList()
        current.add(0, entry)
        val array = JSONArray()
        current.take(50).forEach {
            array.put(
                JSONObject()
                    .put("subject", it.subject)
                    .put("score", it.score)
                    .put("total", it.total)
                    .put("timestamp", it.timestamp)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, array.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
    }
}

@Composable
private fun EstudeNoahApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var subjectName by rememberSaveable { mutableStateOf<String?>(null) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var firstAttemptAlreadyUsed by rememberSaveable { mutableStateOf(false) }
    var solved by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var finalScore by rememberSaveable { mutableIntStateOf(0) }

    val screen = AppScreen.valueOf(screenName)
    val selectedSubject = subjectName?.let { runCatching { Subject.valueOf(it) }.getOrNull() }

    fun goHome() {
        screenName = AppScreen.HOME.name
        subjectName = null
        questionIndex = 0
        score = 0
        firstAttemptAlreadyUsed = false
        solved = false
        feedback = null
    }

    fun startSubject(subject: Subject) {
        subjectName = subject.name
        questionIndex = 0
        score = 0
        firstAttemptAlreadyUsed = false
        solved = false
        feedback = null
        screenName = AppScreen.QUIZ.name
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        when (screen) {
            AppScreen.SUBJECTS -> goHome()
            AppScreen.QUIZ -> screenName = AppScreen.SUBJECTS.name
            AppScreen.RESULT -> goHome()
            AppScreen.HISTORY -> goHome()
            AppScreen.HOME -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                history = HistoryStorage.load(context),
                onStart = { screenName = AppScreen.SUBJECTS.name },
                onHistory = { screenName = AppScreen.HISTORY.name }
            )

            AppScreen.SUBJECTS -> SubjectScreen(
                onBack = ::goHome,
                onSelect = ::startSubject
            )

            AppScreen.QUIZ -> {
                val subject = selectedSubject
                if (subject == null) {
                    goHome()
                } else {
                    val questions = QuestionBank.get(subject)
                    val question = questions.getOrNull(questionIndex)
                    if (question == null) {
                        goHome()
                    } else {
                        QuizScreen(
                            subject = subject,
                            question = question,
                            questionNumber = questionIndex + 1,
                            totalQuestions = questions.size,
                            solved = solved,
                            feedback = feedback,
                            firstAttemptAlreadyUsed = firstAttemptAlreadyUsed,
                            onBack = { screenName = AppScreen.SUBJECTS.name },
                            onAnswer = { optionIndex ->
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
                            onNext = {
                                if (questionIndex == questions.lastIndex) {
                                    finalScore = score
                                    HistoryStorage.add(
                                        context,
                                        HistoryEntry(
                                            subject = subject.label,
                                            score = score,
                                            total = questions.size,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                    screenName = AppScreen.RESULT.name
                                } else {
                                    questionIndex += 1
                                    firstAttemptAlreadyUsed = false
                                    solved = false
                                    feedback = null
                                }
                            }
                        )
                    }
                }
            }

            AppScreen.RESULT -> ResultScreen(
                subject = selectedSubject,
                score = finalScore,
                total = selectedSubject?.let { QuestionBank.get(it).size } ?: 5,
                onAgain = { screenName = AppScreen.SUBJECTS.name },
                onHome = ::goHome,
                onHistory = { screenName = AppScreen.HISTORY.name }
            )

            AppScreen.HISTORY -> HistoryScreen(
                entries = HistoryStorage.load(context),
                onBack = ::goHome,
                onClear = {
                    HistoryStorage.clear(context)
                    screenName = AppScreen.HOME.name
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    history: List<HistoryEntry>,
    onStart: () -> Unit,
    onHistory: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.widthIn(max = 1000.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WelcomeBlock(modifier = Modifier.weight(1f))
                HomeActions(
                    modifier = Modifier.weight(1f),
                    history = history,
                    onStart = onStart,
                    onHistory = onHistory
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeBlock()
                Spacer(Modifier.height(28.dp))
                HomeActions(history = history, onStart = onStart, onHistory = onHistory)
            }
        }
    }
}

@Composable
private fun WelcomeBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(BlueSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("N", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Blue)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Estude, Noah!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cinco questões por vez. Um passo de cada vez.",
            style = MaterialTheme.typography.titleMedium,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeActions(
    history: List<HistoryEntry>,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Começar atividade", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Histórico", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            val last = history.first()
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Última atividade", color = Muted, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(last.subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${last.score}/${last.total} acertos de primeira", color = Green, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectScreen(
    onBack: () -> Unit,
    onSelect: (Subject) -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Escolha a matéria", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            val cardWidth = if (maxWidth >= 700.dp) 280.dp else maxWidth
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Subject.entries.forEach { subject ->
                    Card(
                        modifier = Modifier
                            .width(cardWidth.coerceAtMost(320.dp))
                            .height(150.dp)
                            .clickable { onSelect(subject) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(subject.symbol, fontSize = 32.sp, color = Blue, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp))
                            Text(subject.label, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuizScreen(
    subject: Subject,
    question: Question,
    questionNumber: Int,
    totalQuestions: Int,
    solved: Boolean,
    feedback: String?,
    firstAttemptAlreadyUsed: Boolean,
    onBack: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val wrongSelections = remember(questionNumber) { mutableStateListOf<Int>() }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text(subject.label, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Matérias") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 820.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Questão $questionNumber de $totalQuestions", color = Muted, fontWeight = FontWeight.Bold)
                    Text("${((questionNumber - 1) * 100 / totalQuestions)}%", color = Muted)
                }
                Spacer(Modifier.height(8.dp))
                ProgressDots(current = questionNumber, total = totalQuestions)
                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        question.prompt,
                        modifier = Modifier.padding(26.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp
                    )
                }
                Spacer(Modifier.height(18.dp))

                question.options.forEachIndexed { index, option ->
                    val wasWrong = index in wrongSelections
                    val isCorrectSolved = solved && index == question.correctIndex
                    val containerColor = when {
                        isCorrectSolved -> GreenSoft
                        wasWrong -> RedSoft
                        else -> Color.White
                    }
                    val textColor = when {
                        isCorrectSolved -> Green
                        wasWrong -> Red
                        else -> Ink
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !solved && !wasWrong) {
                                if (index != question.correctIndex) wrongSelections.add(index)
                                onAnswer(index)
                            },
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(BlueSoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(('A'.code + index).toChar().toString(), color = Blue, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(option, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        }
                    }
                }

                if (feedback != null) {
                    Spacer(Modifier.height(14.dp))
                    val success = solved
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (success) GreenSoft else RedSoft),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                feedback,
                                color = if (success) Green else Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            if (success) {
                                Spacer(Modifier.height(6.dp))
                                Text(question.explanation, color = Ink)
                                if (firstAttemptAlreadyUsed) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Você chegou à resposta. Na pontuação, conta o primeiro palpite.", color = Muted)
                                }
                            }
                        }
                    }
                }

                if (solved) {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            if (questionNumber == totalQuestions) "Ver resultado" else "Próxima questão",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (index + 1 == current) 38.dp else 18.dp)
                    .background(
                        if (index + 1 <= current) Blue else Color(0xFFD7DCE8),
                        RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}

@Composable
private fun ResultScreen(
    subject: Subject?,
    score: Int,
    total: Int,
    onAgain: () -> Unit,
    onHome: () -> Unit,
    onHistory: () -> Unit
) {
    val percentage = if (total == 0) 0 else (score * 100 / total)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(if (percentage >= 60) GreenSoft else BlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$percentage%", fontSize = 34.sp, fontWeight = FontWeight.Black, color = if (percentage >= 60) Green else Blue)
            }
            Spacer(Modifier.height(24.dp))
            Text("Atividade concluída!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subject?.label ?: "Atividade", color = Muted, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))
            Text("$score de $total acertos de primeira", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = onAgain,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Fazer outra atividade", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Ver histórico")
            }
            TextButton(onClick = onHome) { Text("Voltar ao início") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    entries: List<HistoryEntry>,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Histórico", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text("Limpar", color = Red) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
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
                items(entries) { entry ->
                    HistoryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: HistoryEntry) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).background(BlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
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
