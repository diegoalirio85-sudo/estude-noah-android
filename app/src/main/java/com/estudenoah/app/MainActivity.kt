package com.estudenoah.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.estudenoah.app.data.local.LocalPreferencesRepository
import com.estudenoah.app.domain.CustomQuestion
import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.MathAnswerEvaluator
import com.estudenoah.app.domain.PreparedActivity
import com.estudenoah.app.domain.Question
import com.estudenoah.app.domain.Subject
import com.estudenoah.app.domain.StudentAnswerRecord
import com.estudenoah.app.material.MaterialFileExtractor
import com.estudenoah.app.network.BackendActivityRepository
import com.estudenoah.app.network.BackendException
import com.estudenoah.app.ui.home.FiveZoneHomeScreen
import com.estudenoah.app.ui.home.MaterialDetailScreen
import com.estudenoah.app.ui.home.TodayMaterial
import com.estudenoah.app.ui.parents.ParentOperationsScreen
import com.estudenoah.app.ui.parents.BackendAccountScreen
import com.estudenoah.app.ui.review.ReviewScreen
import com.estudenoah.app.ui.trophy.TrophyScreen
import android.os.Bundle
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

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

private enum class AppScreen {
    HOME,
    SUBJECTS,
    QUIZ,
    RESULT,
    HISTORY,
    PARENT_PIN,
    PARENT_HOME,
    PARENT_QUESTIONS,
    QUESTION_EDITOR,
    CHANGE_PIN,
    BACKEND_ACCOUNT,
    MATERIAL_INPUT,
    MATERIAL_PREVIEW,
    REVIEW,
    TROPHIES,
    MATERIAL_DETAIL
}

private object QuestionBank {
    private val questions = mapOf(
        Subject.PORTUGUES to listOf(
            Question("pt-1", "Qual palavra está escrita corretamente?", listOf("Caza", "Casa", "Cassa", "Kasa"), 1, "Casa é escrita com S entre as vogais."),
            Question("pt-2", "Na frase “O menino correu rápido”, qual palavra indica uma ação?", listOf("menino", "rápido", "correu", "o"), 2, "“Correu” é o verbo da frase e indica a ação praticada pelo menino."),
            Question("pt-3", "Qual é o plural de “animal”?", listOf("animals", "animales", "animais", "animãos"), 2, "O plural correto de animal é animais."),
            Question("pt-4", "Qual sinal usamos normalmente ao final de uma pergunta?", listOf("!", ".", ",", "?"), 3, "O ponto de interrogação (?) indica uma pergunta."),
            Question("pt-5", "Em “A bola azul caiu”, qual palavra caracteriza a bola?", listOf("azul", "caiu", "a", "bola"), 0, "“Azul” informa uma característica da bola.")
        ),
        Subject.MATEMATICA to listOf(
            Question("mat-1", "Quanto é 8 + 7?", listOf("13", "14", "15", "16"), 2, "8 + 7 = 15."),
            Question("mat-2", "Quanto é 6 × 4?", listOf("10", "20", "24", "28"), 2, "6 grupos de 4 formam 24."),
            Question("mat-3", "Qual número vem imediatamente antes de 100?", listOf("98", "99", "101", "90"), 1, "O número imediatamente anterior a 100 é 99."),
            Question("mat-4", "Uma dúzia corresponde a quantas unidades?", listOf("6", "10", "12", "20"), 2, "Uma dúzia é um conjunto de 12 unidades."),
            Question("mat-5", "Se você tinha 20 figurinhas e deu 5, com quantas ficou?", listOf("10", "15", "20", "25"), 1, "20 − 5 = 15.")
        ),
        Subject.CIENCIAS to listOf(
            Question("cie-1", "Qual destes animais é um mamífero?", listOf("Galinha", "Cachorro", "Tartaruga", "Sardinha"), 1, "O cachorro é um mamífero: nasce do corpo da mãe e, quando filhote, mama."),
            Question("cie-2", "Qual parte da planta normalmente absorve água do solo?", listOf("Raiz", "Flor", "Fruto", "Folha"), 0, "As raízes fixam a planta e absorvem água e sais minerais do solo."),
            Question("cie-3", "Qual destes elementos é essencial para respirarmos?", listOf("Areia", "Oxigênio", "Plástico", "Vidro"), 1, "Nosso corpo utiliza o oxigênio presente no ar durante a respiração."),
            Question("cie-4", "Em qual estado físico a água está quando vira gelo?", listOf("Gasoso", "Líquido", "Sólido", "Vapor"), 2, "O gelo é água no estado sólido."),
            Question("cie-5", "Qual astro ilumina naturalmente a Terra durante o dia?", listOf("Lua", "Marte", "Sol", "Saturno"), 2, "O Sol é a estrela que fornece luz e calor à Terra.")
        ),
        Subject.HISTORIA to listOf(
            Question("his-1", "Para estudar o passado, os historiadores utilizam principalmente:", listOf("fontes históricas", "previsões do tempo", "apenas mapas", "somente números"), 0, "Fontes históricas podem ser documentos, objetos, imagens, relatos e muitos outros vestígios."),
            Question("his-2", "Uma fotografia antiga pode ser considerada:", listOf("um brinquedo", "uma fonte histórica", "um planeta", "uma operação matemática"), 1, "Fotografias registram pessoas, lugares e acontecimentos de uma época."),
            Question("his-3", "Quando organizamos acontecimentos do mais antigo para o mais recente, fazemos uma:", listOf("receita", "linha do tempo", "tabuada", "legenda"), 1, "A linha do tempo ajuda a visualizar a ordem cronológica dos acontecimentos."),
            Question("his-4", "Os relatos de pessoas que viveram um acontecimento são exemplos de:", listOf("fontes orais", "fontes minerais", "formas geométricas", "medidas de massa"), 0, "Depoimentos e entrevistas são fontes orais."),
            Question("his-5", "Estudar a história de uma família ajuda a compreender:", listOf("apenas o futuro", "mudanças e permanências ao longo do tempo", "somente contas", "apenas animais"), 1, "A história permite observar o que mudou e o que permaneceu ao longo do tempo.")
        ),
        Subject.GEOGRAFIA to listOf(
            Question("geo-1", "Qual representação mostra a superfície de um lugar vista de cima?", listOf("Mapa", "Poema", "Receita", "Canção"), 0, "Mapas representam espaços e ajudam a localizar lugares."),
            Question("geo-2", "Em qual planeta nós vivemos?", listOf("Marte", "Terra", "Vênus", "Júpiter"), 1, "Vivemos no planeta Terra."),
            Question("geo-3", "Uma área com muitos prédios, ruas e comércio é geralmente uma paisagem:", listOf("urbana", "marinha", "desértica", "polar"), 0, "Paisagens urbanas são marcadas pela concentração de construções e atividades da cidade."),
            Question("geo-4", "Qual ponto cardeal é indicado pela letra N?", listOf("Sul", "Leste", "Oeste", "Norte"), 3, "A letra N representa o Norte."),
            Question("geo-5", "Rios, montanhas e vegetação são exemplos de elementos:", listOf("naturais", "digitais", "musicais", "numéricos"), 0, "Esses elementos fazem parte da natureza e compõem as paisagens.")
        )
    )

    fun activity(context: Context, subject: Subject): List<Question> {
        val custom = LocalPreferencesRepository(context).loadCustomQuestions()
            .filter { it.subject == subject }
            .map { it.asQuestion() }
            .shuffled()
        val builtIn = questions[subject].orEmpty().shuffled()
        return (custom + builtIn).take(5)
    }
}

private object QuestionJson {
    fun encode(questions: List<Question>): String {
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

    fun decode(raw: String): List<Question> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val optionArray = item.getJSONArray("options")
                val options = buildList {
                    for (j in 0 until optionArray.length()) add(optionArray.getString(j))
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
}


private object MaterialQuestionGenerator {
    private val sentenceSplit = Regex("(?<=[.!?])\\s+|\\n+")
    private val spaces = Regex("\\s+")
    private val wordRegex = Regex("[\\p{L}À-ÿ][\\p{L}À-ÿ'’-]{3,}")
    private val numberRegex = Regex("\\b\\d{1,5}\\b")
    private val stopWords = setOf(
        "aquela", "aquele", "aqueles", "aquelas", "ainda", "algum", "alguma", "alguns", "algumas",
        "assim", "cada", "como", "com", "contra", "depois", "desde", "dessa", "desse", "desta", "deste",
        "durante", "ela", "elas", "ele", "eles", "entre", "essa", "essas", "esse", "esses", "esta", "estas",
        "este", "estes", "está", "estão", "foram", "mais", "mas", "mesma", "mesmo", "muito", "muita", "muitos",
        "muitas", "não", "nossa", "nosso", "numa", "nunca", "onde", "outra", "outro", "para", "pela", "pelas",
        "pelo", "pelos", "porque", "quando", "qual", "quais", "quem", "seja", "sendo", "será", "sobre", "também",
        "tem", "tendo", "toda", "todas", "todo", "todos", "uma", "umas", "uns", "vários", "várias", "isso", "isto",
        "eram", "era", "foi", "são", "ser", "seu", "sua", "seus", "suas", "dos", "das", "nas", "nos", "por"
    )

    fun generate(text: String, subject: Subject, count: Int = 5, salt: Long = 0L): List<Question> {
        val normalized = text.replace("\\r", "").trim()
        if (normalized.length < 140) return emptyList()
        return if (subject == Subject.MATEMATICA) {
            generateMath(normalized, count, salt)
        } else {
            generateTrueFalse(normalized, subject, count, salt)
        }
    }

    private fun cleanSentences(text: String): List<String> = text
        .split(sentenceSplit)
        .map { it.replace(spaces, " ").trim().trim('•', '-', '–', '—') }
        .filter { it.length in 28..360 && it.split(spaces).size >= 5 }
        .distinctBy { it.lowercase(Locale.getDefault()) }

    private fun contentWords(sentence: String): List<String> = wordRegex.findAll(sentence)
        .map { it.value }
        .filter { word ->
            val lower = word.lowercase(Locale.getDefault())
            lower !in stopWords && lower.length >= 4 && lower.any { it.isLetter() }
        }
        .toList()

    private fun generateTrueFalse(text: String, subject: Subject, count: Int, salt: Long): List<Question> {
        val sentences = cleanSentences(text)
        if (sentences.size < 2) return emptyList()

        val vocabulary = sentences
            .flatMap(::contentWords)
            .distinctBy { it.lowercase(Locale.getDefault()) }

        val seed = text.hashCode().toLong() xor subject.name.hashCode().toLong() xor salt
        val random = kotlin.random.Random(seed)
        val ordered = sentences.shuffled(random)
        val results = mutableListOf<Question>()

        repeat(count) { index ->
            val source = ordered[index % ordered.size]
            // Mantém um conjunto equilibrado: 3 verdadeiras e 2 falsas, variando a ordem a cada regeneração.
            val shouldBeFalse = ((index + kotlin.math.abs((seed % 5).toInt())) % 5) in setOf(1, 3)
            val statement = if (shouldBeFalse) makeFalseStatement(source, vocabulary, random) else source
            val actuallyFalse = shouldBeFalse && statement != source
            val correctIndex = if (actuallyFalse) 1 else 0
            val normalizedStatement = statement.trim().let {
                if (it.endsWith('.') || it.endsWith('!') || it.endsWith('?')) it else "$it."
            }

            results += Question(
                id = "vf-${subject.name.lowercase()}-${source.hashCode()}-$index-$salt",
                prompt = normalizedStatement,
                options = listOf("Verdadeiro", "Falso"),
                correctIndex = correctIndex,
                explanation = if (correctIndex == 0) {
                    "Verdadeiro. O material informa: “$source”"
                } else {
                    "Falso. No material, a informação correta é: “$source”"
                }
            )
        }
        return results
    }

    private fun makeFalseStatement(source: String, vocabulary: List<String>, random: kotlin.random.Random): String {
        val numberMatch = numberRegex.find(source)
        if (numberMatch != null) {
            val original = numberMatch.value.toIntOrNull()
            if (original != null) {
                val delta = if (original <= 10) 1 + random.nextInt(3) else 2 + random.nextInt(8)
                val replacement = (original + delta).toString()
                return source.replaceRange(numberMatch.range, replacement)
            }
        }

        val verbWords = "é|são|foi|foram|está|estão|tem|têm|possui|possuem|ocorre|ocorrem|pode|podem|deve|devem|gira|giram|vive|vivem|fica|ficam|produz|produzem|fornece|fornecem|usa|usam|utiliza|utilizam|apresenta|apresentam|pertence|pertencem|representa|representam|absorve|absorvem|realiza|realizam|forma|formam|causa|causam|indica|indicam|corresponde|correspondem|localiza|localizam|nasce|nascem|morre|morrem"
        val alreadyNegative = Regex("\\bnão\\s+($verbWords)\\b", RegexOption.IGNORE_CASE).find(source)
        if (alreadyNegative != null) {
            val positiveVerb = alreadyNegative.groupValues[1]
            return source.replaceRange(alreadyNegative.range, positiveVerb)
        }

        val verbRegex = Regex("\\b($verbWords)\\b", RegexOption.IGNORE_CASE)
        val verb = verbRegex.find(source)
        if (verb != null) {
            return source.replaceRange(verb.range, "não ${verb.value}")
        }

        val sourceWords = contentWords(source)
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedByDescending { it.length }

        for (target in sourceWords) {
            val targetLower = target.lowercase(Locale.getDefault())
            val replacements = vocabulary.filter { candidate ->
                val lower = candidate.lowercase(Locale.getDefault())
                lower != targetLower && !source.contains(candidate, ignoreCase = true)
            }
            if (replacements.isNotEmpty()) {
                val replacement = replacements.shuffled(random).first()
                val regex = Regex("\\b${Regex.escape(target)}\\b", RegexOption.IGNORE_CASE)
                val changed = regex.replaceFirst(source, replacement)
                if (changed != source) return changed
            }
        }

        return source
    }

    private enum class MathTopic { ADD, SUBTRACT, MULTIPLY, DIVIDE, FRACTION, PERCENT, AREA, PERIMETER, MIXED }

    private fun generateMath(text: String, count: Int, salt: Long): List<Question> {
        val lower = text.lowercase(Locale.getDefault())
        val topic = when {
            listOf("perímetro", "perimetro").any { it in lower } -> MathTopic.PERIMETER
            listOf("área", "area", "quadrado", "retângulo", "retangulo").any { it in lower } -> MathTopic.AREA
            listOf("porcent", "percent", "%").any { it in lower } -> MathTopic.PERCENT
            listOf("fração", "fracao", "frações", "fracoes", "numerador", "denominador").any { it in lower } -> MathTopic.FRACTION
            listOf("multiplica", "tabuada", "produto", "vezes").any { it in lower } -> MathTopic.MULTIPLY
            listOf("divis", "quociente", "dividir").any { it in lower } -> MathTopic.DIVIDE
            listOf("subtra", "diferença", "diferenca", "menos").any { it in lower } -> MathTopic.SUBTRACT
            listOf("adição", "adicao", "soma", "somar").any { it in lower } -> MathTopic.ADD
            else -> MathTopic.MIXED
        }

        val numbersFromMaterial = numberRegex.findAll(text)
            .mapNotNull { it.value.toIntOrNull() }
            .filter { it in 1..9999 }
            .distinct()
            .toList()

        val seed = text.hashCode().toLong() xor salt xor 0x4D415448L
        val random = kotlin.random.Random(seed)
        val baseNumbers = if (numbersFromMaterial.isNotEmpty()) numbersFromMaterial else listOf(2, 3, 4, 5, 6, 8, 10, 12)

        fun pick(index: Int, min: Int = 2, max: Int = 40): Int {
            val raw = baseNumbers[index % baseNumbers.size]
            val adjusted = if (raw in min..max) raw else min + kotlin.math.abs(raw % (max - min + 1))
            return adjusted.coerceIn(min, max)
        }

        val results = mutableListOf<Question>()
        repeat(count) { index ->
            val effectiveTopic = if (topic == MathTopic.MIXED) {
                listOf(MathTopic.ADD, MathTopic.SUBTRACT, MathTopic.MULTIPLY, MathTopic.DIVIDE, MathTopic.FRACTION)[index % 5]
            } else topic

            val a = pick(index * 2, 2, 50)
            val b = pick(index * 2 + 1, 2, 25)
            val question = when (effectiveTopic) {
                MathTopic.ADD -> {
                    val answer = a + b
                    numericQuestion(
                        id = "math-add-$index-$salt",
                        prompt = "Resolva: $a + $b = ?",
                        answer = answer,
                        explanation = "$a + $b = $answer.",
                        random = random
                    )
                }
                MathTopic.SUBTRACT -> {
                    val high = maxOf(a, b) + index
                    val low = minOf(a, b)
                    val answer = high - low
                    numericQuestion(
                        id = "math-sub-$index-$salt",
                        prompt = "Resolva: $high − $low = ?",
                        answer = answer,
                        explanation = "$high − $low = $answer.",
                        random = random
                    )
                }
                MathTopic.MULTIPLY -> {
                    val x = a.coerceIn(2, 12)
                    val y = b.coerceIn(2, 12)
                    val answer = x * y
                    numericQuestion(
                        id = "math-mul-$index-$salt",
                        prompt = "Resolva: $x × $y = ?",
                        answer = answer,
                        explanation = "$x × $y = $answer.",
                        random = random
                    )
                }
                MathTopic.DIVIDE -> {
                    val divisor = b.coerceIn(2, 12)
                    val quotient = a.coerceIn(2, 12)
                    val dividend = divisor * quotient
                    numericQuestion(
                        id = "math-div-$index-$salt",
                        prompt = "Resolva: $dividend ÷ $divisor = ?",
                        answer = quotient,
                        explanation = "$dividend ÷ $divisor = $quotient.",
                        random = random
                    )
                }
                MathTopic.FRACTION -> {
                    val denominator = (b.coerceIn(3, 12)).coerceAtLeast(3)
                    val numerator = (a % (denominator - 1)).coerceAtLeast(1)
                    val answer = "$numerator/$denominator"
                    val distractors = linkedSetOf<String>()
                    distractors += "$denominator/$numerator"
                    distractors += "${(numerator + 1).coerceAtMost(denominator)}/$denominator"
                    distractors += "$numerator/${denominator + 1}"
                    distractors.remove(answer)
                    val options = (distractors.take(3) + answer).shuffled(random)
                    Question(
                        id = "math-frac-$index-$salt",
                        prompt = "Uma figura foi dividida em $denominator partes iguais e $numerator parte${if (numerator == 1) "" else "s"} foi${if (numerator == 1) "" else "ram"} destacada${if (numerator == 1) "" else "s"}. Qual fração representa a parte destacada?",
                        options = options,
                        correctIndex = options.indexOf(answer),
                        explanation = "São $numerator partes destacadas de um total de $denominator: $answer."
                    )
                }
                MathTopic.PERCENT -> {
                    val percentages = listOf(10, 20, 25, 50)
                    val pct = percentages[index % percentages.size]
                    val base = ((a.coerceAtLeast(10) + 9) / 10) * 10
                    val answer = base * pct / 100
                    numericQuestion(
                        id = "math-pct-$index-$salt",
                        prompt = "Quanto é $pct% de $base?",
                        answer = answer,
                        explanation = "$pct% de $base = $answer.",
                        random = random
                    )
                }
                MathTopic.AREA -> {
                    val width = a.coerceIn(2, 15)
                    val height = b.coerceIn(2, 15)
                    val answer = width * height
                    numericQuestion(
                        id = "math-area-$index-$salt",
                        prompt = "Um retângulo mede $width unidades de comprimento e $height de largura. Qual é a área?",
                        answer = answer,
                        explanation = "Área do retângulo = $width × $height = $answer unidades quadradas.",
                        random = random
                    )
                }
                MathTopic.PERIMETER -> {
                    val width = a.coerceIn(2, 15)
                    val height = b.coerceIn(2, 15)
                    val answer = 2 * (width + height)
                    numericQuestion(
                        id = "math-per-$index-$salt",
                        prompt = "Um retângulo tem lados de $width e $height unidades. Qual é o perímetro?",
                        answer = answer,
                        explanation = "Perímetro = 2 × ($width + $height) = $answer unidades.",
                        random = random
                    )
                }
                MathTopic.MIXED -> error("Tema misto deve ser resolvido antes")
            }
            results += question
        }
        return results
    }

    private fun numericQuestion(
        id: String,
        prompt: String,
        answer: Int,
        explanation: String,
        random: kotlin.random.Random
    ): Question {
        val distractors = linkedSetOf<Int>()
        val offsets = listOf(-2, -1, 1, 2, 3, 5, -5).shuffled(random)
        for (offset in offsets) {
            val candidate = answer + offset
            if (candidate >= 0 && candidate != answer) distractors += candidate
            if (distractors.size >= 3) break
        }
        var fallback = answer + 6
        while (distractors.size < 3) {
            if (fallback >= 0 && fallback != answer) distractors += fallback
            fallback += 2
        }
        val options = (distractors.take(3).map { it.toString() } + answer.toString()).shuffled(random)
        return Question(
            id = id,
            prompt = prompt,
            options = options,
            correctIndex = options.indexOf(answer.toString()),
            explanation = explanation
        )
    }
}

@Composable
private fun EstudeNoahApp() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var homeDataVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) homeDataVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val localPreferences = LocalPreferencesRepository(context)
    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var subjectName by rememberSaveable { mutableStateOf<String?>(null) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var firstAttemptAlreadyUsed by rememberSaveable { mutableStateOf(false) }
    var solved by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var finalScore by rememberSaveable { mutableIntStateOf(0) }
    var activeQuestionsJson by rememberSaveable { mutableStateOf("[]") }
    var editingQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
    var materialTitle by rememberSaveable { mutableStateOf("") }
    var materialText by rememberSaveable { mutableStateOf("") }
    var materialSubjectName by rememberSaveable { mutableStateOf(Subject.PORTUGUES.name) }
    var materialQuestionsJson by rememberSaveable { mutableStateOf("[]") }
    var materialSalt by rememberSaveable { mutableStateOf(0L) }
    var quizReturnScreenName by rememberSaveable { mutableStateOf(AppScreen.SUBJECTS.name) }
    var historySubjectLabel by rememberSaveable { mutableStateOf("") }
    var activePreparedId by rememberSaveable { mutableStateOf<String?>(null) }
    var studentAnswers by remember { mutableStateOf<List<StudentAnswerRecord>>(emptyList()) }
    var selectedHomeMaterial by remember { mutableStateOf<TodayMaterial?>(null) }

    val screen = AppScreen.valueOf(screenName)
    val selectedSubject = subjectName?.let { runCatching { Subject.valueOf(it) }.getOrNull() }
    val activeQuestions = QuestionJson.decode(activeQuestionsJson)

    fun goHome() {
        screenName = AppScreen.HOME.name
        subjectName = null
        questionIndex = 0
        score = 0
        firstAttemptAlreadyUsed = false
        solved = false
        feedback = null
        activeQuestionsJson = "[]"
        editingQuestionId = null
        materialTitle = ""
        materialText = ""
        materialQuestionsJson = "[]"
        materialSalt = 0L
        quizReturnScreenName = AppScreen.SUBJECTS.name
        historySubjectLabel = ""
        activePreparedId = null
        studentAnswers = emptyList()
    }

    fun startActivity(
        subject: Subject,
        questions: List<Question>,
        returnScreen: AppScreen,
        historyLabel: String,
        preparedId: String? = null
    ) {
        subjectName = subject.name
        activeQuestionsJson = QuestionJson.encode(questions)
        questionIndex = 0
        score = 0
        firstAttemptAlreadyUsed = false
        solved = false
        feedback = null
        quizReturnScreenName = returnScreen.name
        historySubjectLabel = historyLabel
        studentAnswers = emptyList()
        activePreparedId = preparedId
        screenName = AppScreen.QUIZ.name
    }

    fun startSubject(subject: Subject) {
        startActivity(
            subject = subject,
            questions = QuestionBank.activity(context, subject),
            returnScreen = AppScreen.SUBJECTS,
            historyLabel = subject.label
        )
    }

    fun startPrepared(activity: PreparedActivity) {
        startActivity(
            subject = activity.subject,
            questions = activity.questions,
            returnScreen = AppScreen.HOME,
            historyLabel = "${activity.subject.label} • ${activity.title}",
            preparedId = activity.id
        )
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        when (screen) {
            AppScreen.SUBJECTS -> goHome()
            AppScreen.QUIZ -> screenName = quizReturnScreenName
            AppScreen.RESULT -> goHome()
            AppScreen.HISTORY -> goHome()
            AppScreen.PARENT_PIN -> goHome()
            AppScreen.PARENT_HOME -> goHome()
            AppScreen.PARENT_QUESTIONS -> screenName = AppScreen.PARENT_HOME.name
            AppScreen.QUESTION_EDITOR -> screenName = AppScreen.PARENT_QUESTIONS.name
            AppScreen.CHANGE_PIN -> screenName = AppScreen.PARENT_HOME.name
            AppScreen.BACKEND_ACCOUNT -> screenName = AppScreen.PARENT_HOME.name
            AppScreen.MATERIAL_INPUT -> screenName = AppScreen.PARENT_HOME.name
            AppScreen.MATERIAL_PREVIEW -> screenName = AppScreen.MATERIAL_INPUT.name
            AppScreen.REVIEW -> goHome()
            AppScreen.TROPHIES -> goHome()
            AppScreen.MATERIAL_DETAIL -> goHome()
            AppScreen.HOME -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        when (screen) {
            AppScreen.HOME -> {
                homeDataVersion // Reavaliado ao retornar do launcher Agenda Vieira ou ao concluir tarefa.
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                val dailyPlan = localPreferences.loadDailyLessonPlan(today)
                FiveZoneHomeScreen(
                    history = localPreferences.loadHistory(),
                    preparedActivity = localPreferences.loadPreparedActivity(),
                    dailyLessonPlan = dailyPlan,
                    homeworkCompletions = localPreferences.loadHomeworkCompletions(today),
                    onHomeworkCompletion = { lesson, completed ->
                        dailyPlan?.let { plan ->
                            localPreferences.setHomeworkCompletion(plan.date, lesson, completed)
                            homeDataVersion++
                        }
                    },
                    onCreateActivity = { screenName = AppScreen.MATERIAL_INPUT.name },
                    onQuickPractice = { screenName = AppScreen.SUBJECTS.name },
                    onReview = { screenName = AppScreen.REVIEW.name },
                    onTrophies = { screenName = AppScreen.TROPHIES.name },
                    onHistory = { screenName = AppScreen.HISTORY.name },
                    onParents = { screenName = AppScreen.PARENT_PIN.name },
                    onPrepared = ::startPrepared,
                    onMaterial = {
                        selectedHomeMaterial = it
                        screenName = AppScreen.MATERIAL_DETAIL.name
                    }
                )
            }

            AppScreen.SUBJECTS -> SubjectScreen(onBack = ::goHome, onSelect = ::startSubject)

            AppScreen.QUIZ -> {
                val subject = selectedSubject
                val question = activeQuestions.getOrNull(questionIndex)
                if (subject == null || question == null) {
                    goHome()
                } else {
                    QuizScreen(
                        subject = subject,
                        question = question,
                        questionNumber = questionIndex + 1,
                        totalQuestions = activeQuestions.size,
                        solved = solved,
                        feedback = feedback,
                        firstAttemptAlreadyUsed = firstAttemptAlreadyUsed,
                        onBack = { screenName = quizReturnScreenName },
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
                        onMathAnswer = { studentAnswer ->
                            if (!solved && question.isMathProblem) {
                                val evaluation = MathAnswerEvaluator.evaluate(studentAnswer, question.mathAnswer.orEmpty())
                                if (evaluation == true) score += 1
                                firstAttemptAlreadyUsed = evaluation != true
                                solved = true
                                feedback = when (evaluation) {
                                    true -> "Resposta correta!"
                                    false -> "Não foi dessa vez."
                                    null -> "Confira sua resposta."
                                }
                                studentAnswers = studentAnswers + StudentAnswerRecord(question.id, studentAnswer.trim(), evaluation)
                            }
                        },
                        onNext = {
                            if (questionIndex == activeQuestions.lastIndex) {
                                finalScore = score
                                localPreferences.addHistory(
                                    HistoryEntry(
                                        subject = historySubjectLabel.ifBlank { subject.label },
                                        score = score,
                                        total = activeQuestions.size,
                                        timestamp = System.currentTimeMillis(),
                                        answers = studentAnswers
                                    )
                                )
                                if (activePreparedId != null) {
                                    localPreferences.clearPreparedActivity()
                                    activePreparedId = null
                                }
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

            AppScreen.RESULT -> ResultScreen(
                subject = selectedSubject,
                score = finalScore,
                total = activeQuestions.size.coerceAtLeast(1),
                onAgain = { screenName = AppScreen.SUBJECTS.name },
                onHome = ::goHome,
                onHistory = { screenName = AppScreen.HISTORY.name }
            )

            AppScreen.HISTORY -> HistoryScreen(
                entries = localPreferences.loadHistory(),
                onBack = ::goHome,
                onClear = {
                    localPreferences.clearHistory()
                    screenName = AppScreen.HOME.name
                }
            )

            AppScreen.PARENT_PIN -> ParentPinScreen(
                expectedPin = localPreferences.getParentPin(),
                onBack = ::goHome,
                onSuccess = { screenName = AppScreen.PARENT_HOME.name }
            )

            AppScreen.PARENT_HOME -> ParentOperationsScreen(
                questionCount = localPreferences.loadCustomQuestions().size,
                onBack = ::goHome,
                onManageQuestions = { screenName = AppScreen.PARENT_QUESTIONS.name },
                onImportMaterial = { screenName = AppScreen.MATERIAL_INPUT.name },
                onChangePin = { screenName = AppScreen.CHANGE_PIN.name },
                onBackendAccount = { screenName = AppScreen.BACKEND_ACCOUNT.name }
            )

            AppScreen.BACKEND_ACCOUNT -> BackendAccountScreen(
                onBack = { screenName = AppScreen.PARENT_HOME.name }
            )

            AppScreen.PARENT_QUESTIONS -> ParentQuestionsScreen(
                initialQuestions = localPreferences.loadCustomQuestions(),
                onBack = { screenName = AppScreen.PARENT_HOME.name },
                onNew = {
                    editingQuestionId = null
                    screenName = AppScreen.QUESTION_EDITOR.name
                },
                onEdit = { id ->
                    editingQuestionId = id
                    screenName = AppScreen.QUESTION_EDITOR.name
                },
                onDelete = { id -> localPreferences.deleteCustomQuestion(id) }
            )

            AppScreen.QUESTION_EDITOR -> {
                val editing = editingQuestionId?.let { id ->
                    localPreferences.loadCustomQuestions().firstOrNull { it.id == id }
                }
                QuestionEditorScreen(
                    editing = editing,
                    onBack = { screenName = AppScreen.PARENT_QUESTIONS.name },
                    onSave = { question ->
                        localPreferences.upsertCustomQuestion(question)
                        editingQuestionId = null
                        screenName = AppScreen.PARENT_QUESTIONS.name
                    }
                )
            }

            AppScreen.MATERIAL_INPUT -> MaterialInputScreen(
                initialSubject = runCatching { Subject.valueOf(materialSubjectName) }.getOrDefault(Subject.PORTUGUES),
                initialTitle = materialTitle,
                initialText = materialText,
                onBack = { screenName = AppScreen.PARENT_HOME.name },
                onGenerate = { subject, title, text, questions ->
                    materialSubjectName = subject.name
                    materialTitle = title
                    materialText = text
                    materialQuestionsJson = QuestionJson.encode(questions)
                    materialSalt = 0L
                    screenName = AppScreen.MATERIAL_PREVIEW.name
                }
            )

            AppScreen.MATERIAL_PREVIEW -> {
                val subject = runCatching { Subject.valueOf(materialSubjectName) }.getOrDefault(Subject.PORTUGUES)
                val questions = QuestionJson.decode(materialQuestionsJson)
                MaterialPreviewScreen(
                    subject = subject,
                    title = materialTitle,
                    questions = questions,
                    onBack = { screenName = AppScreen.MATERIAL_INPUT.name },
                    onRegenerate = {
                        screenName = AppScreen.MATERIAL_INPUT.name
                    },
                    onSave = {
                        localPreferences.savePreparedActivity(
                            PreparedActivity(
                                id = UUID.randomUUID().toString(),
                                title = materialTitle,
                                subject = subject,
                                sourceText = materialText,
                                questions = questions,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        goHome()
                    },
                    onTest = {
                        startActivity(
                            subject = subject,
                            questions = questions,
                            returnScreen = AppScreen.PARENT_HOME,
                            historyLabel = "${subject.label} • ${materialTitle}"
                        )
                    }
                )
            }

            AppScreen.CHANGE_PIN -> ChangePinScreen(
                currentPin = localPreferences.getParentPin(),
                onBack = { screenName = AppScreen.PARENT_HOME.name },
                onSave = { pin ->
                    localPreferences.setParentPin(pin)
                    screenName = AppScreen.PARENT_HOME.name
                }
            )

            AppScreen.REVIEW -> ReviewScreen(
                onBack = ::goHome,
                onStart = { screenName = AppScreen.SUBJECTS.name }
            )

            AppScreen.TROPHIES -> TrophyScreen(
                history = localPreferences.loadHistory(),
                onBack = ::goHome
            )

            AppScreen.MATERIAL_DETAIL -> selectedHomeMaterial?.let {
                MaterialDetailScreen(material = it, onBack = ::goHome)
            } ?: goHome()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectScreen(onBack: () -> Unit, onSelect: (Subject) -> Unit) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Escolha a matéria", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            val cardWidth = if (maxWidth >= 700.dp) 280.dp else maxWidth
            FlowRow(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Subject.entries.forEach { subject ->
                    Card(
                        modifier = Modifier.width(cardWidth.coerceAtMost(320.dp)).height(150.dp).clickable { onSelect(subject) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
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
    onMathAnswer: (String) -> Unit,
    onNext: () -> Unit
) {
    val wrongSelections = remember(question.id) { mutableStateListOf<Int>() }
    val trueFalse = question.options == listOf("Verdadeiro", "Falso")
    val mathProblem = question.isMathProblem
    val feedbackIsCorrect = !mathProblem || feedback == "Resposta correta!"
    var mathInput by rememberSaveable(question.id) { mutableStateOf("") }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text(subject.label, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Matérias") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (trueFalse) "Afirmação $questionNumber de $totalQuestions" else "Questão $questionNumber de $totalQuestions",
                        color = Muted,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${((questionNumber - 1) * 100 / totalQuestions)}%", color = Muted)
                }
                Spacer(Modifier.height(8.dp))
                ProgressDots(current = questionNumber, total = totalQuestions)
                Spacer(Modifier.height(24.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                    Text(question.prompt, modifier = Modifier.padding(26.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                }
                Spacer(Modifier.height(18.dp))

                if (mathProblem) {
                    OutlinedTextField(
                        value = mathInput,
                        onValueChange = { mathInput = it.take(80) },
                        enabled = !solved,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(MathAnswerEvaluator.INPUT_LABEL) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (MathAnswerEvaluator.prefersNumericKeyboard(question.mathAnswer.orEmpty())) KeyboardType.Decimal else KeyboardType.Text
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onMathAnswer(mathInput) },
                        enabled = !solved && mathInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(MathAnswerEvaluator.SUBMIT_LABEL, fontWeight = FontWeight.Bold) }
                }

                if (!mathProblem) question.options.forEachIndexed { index, option ->
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(enabled = !solved && !wasWrong) {
                            if (index != question.correctIndex) wrongSelections.add(index)
                            onAnswer(index)
                        },
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(38.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                                Text(('A'.code + index).toChar().toString(), color = Blue, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(option, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                        }
                    }
                }

                if (feedback != null) {
                    Spacer(Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (solved && feedbackIsCorrect) GreenSoft else RedSoft),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(feedback, color = if (solved && feedbackIsCorrect) Green else Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (solved && mathProblem) {
                                Spacer(Modifier.height(8.dp))
                                Text("Resposta correta: ${question.mathAnswer}", color = Ink, fontWeight = FontWeight.Bold)
                                if (question.explanation.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(question.explanation, color = Ink)
                                }
                                question.solutionSteps.forEachIndexed { index, step ->
                                    Spacer(Modifier.height(4.dp))
                                    Text("${index + 1}. $step", color = Ink)
                                }
                            } else if (solved) {
                                if (question.explanation.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(question.explanation, color = Ink)
                                }
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
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                        Text(
                            if (questionNumber == totalQuestions) "Ver resultado" else if (trueFalse) "Próxima afirmação" else "Próxima questão",
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
                modifier = Modifier.height(8.dp).width(if (index + 1 == current) 38.dp else 18.dp)
                    .background(if (index + 1 <= current) Blue else Color(0xFFD7DCE8), RoundedCornerShape(99.dp))
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
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(130.dp).background(if (percentage >= 60) GreenSoft else BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                Text("$percentage%", fontSize = 34.sp, fontWeight = FontWeight.Black, color = if (percentage >= 60) Green else Blue)
            }
            Spacer(Modifier.height(24.dp))
            Text("Atividade concluída!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subject?.label ?: "Atividade", color = Muted, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))
            Text("$score de $total acertos de primeira", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(30.dp))
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Fazer outra atividade", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Text("Ver histórico") }
            TextButton(onClick = onHome) { Text("Voltar ao início") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentPinScreen(expectedPin: String, onBack: () -> Unit, onSuccess: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Área dos Pais", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(72.dp).background(BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                        Text("🔒", fontSize = 30.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Acesso dos responsáveis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Digite o PIN para cadastrar e gerenciar perguntas.", color = Muted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { value ->
                            pin = value.filter { it.isDigit() }.take(8)
                            error = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("PIN") },
                        singleLine = true,
                        isError = error,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (error) {
                        Spacer(Modifier.height(6.dp))
                        Text("PIN incorreto.", color = Red, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (pin == expectedPin) onSuccess() else error = true
                        },
                        enabled = pin.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Entrar", fontWeight = FontWeight.Bold) }
                    if (expectedPin == "1234") {
                        Spacer(Modifier.height(16.dp))
                        Text("Primeiro acesso: PIN 1234. Você poderá alterá-lo depois.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentHomeScreen(
    questions: List<CustomQuestion>,
    onBack: () -> Unit,
    onNewQuestion: () -> Unit,
    onManage: () -> Unit,
    onNewMaterial: () -> Unit,
    onChangePin: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Área dos Pais", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()).widthIn(max = 900.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BlueSoft), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Perguntas personalizadas", color = Blue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${questions.size} cadastrada${if (questions.size == 1) "" else "s"}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("As perguntas cadastradas aqui entram primeiro nas atividades. Cada atividade continua com 5 questões.", color = Muted)
                }
            }
            Spacer(Modifier.height(18.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Subject.entries.forEach { subject ->
                    val count = questions.count { it.subject == subject }
                    Card(modifier = Modifier.width(155.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(subject.symbol, color = Blue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text(subject.label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("$count", color = Green, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onNewMaterial, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Criar atividade a partir de material", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onNewQuestion, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("+ Cadastrar pergunta manualmente", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Gerenciar perguntas", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onChangePin) { Text("Alterar PIN") }
            Spacer(Modifier.height(10.dp))
            Text("Versão 3.3", color = Muted, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentQuestionsScreen(
    initialQuestions: List<CustomQuestion>,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var questions by remember(initialQuestions) { mutableStateOf(initialQuestions) }
    var selectedSubjectName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<CustomQuestion?>(null) }
    val selectedSubject = selectedSubjectName?.let { runCatching { Subject.valueOf(it) }.getOrNull() }
    val filtered = questions.filter { selectedSubject == null || it.subject == selectedSubject }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir pergunta?") },
            text = { Text("Esta pergunta será removida das próximas atividades.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = pendingDelete!!.id
                    onDelete(id)
                    questions = questions.filterNot { it.id == id }
                    pendingDelete = null
                }) { Text("Excluir", color = Red) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Perguntas", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Pais") } },
                actions = { TextButton(onClick = onNew) { Text("+ Nova") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterButton("Todas", selectedSubject == null) { selectedSubjectName = null }
                Subject.entries.forEach { subject ->
                    FilterButton(subject.label, selectedSubject == subject) { selectedSubjectName = subject.name }
                }
            }
            HorizontalDivider()
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nenhuma pergunta cadastrada aqui.", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Toque em “+ Nova” para criar a primeira.", color = Muted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(filtered, key = { it.id }) { question ->
                        CustomQuestionCard(question, onEdit = { onEdit(question.id) }, onDelete = { pendingDelete = question })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, shape = RoundedCornerShape(99.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(99.dp)) { Text(label) }
    }
}

@Composable
private fun CustomQuestionCard(question: CustomQuestion, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().widthIn(max = 820.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(question.subject.label, color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = Red) }
            }
            Text(question.prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            question.options.forEachIndexed { index, option ->
                Text("${('A'.code + index).toChar()}) $option", color = if (index == question.correctIndex) Green else Muted, fontWeight = if (index == question.correctIndex) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditorScreen(editing: CustomQuestion?, onBack: () -> Unit, onSave: (CustomQuestion) -> Unit) {
    var subjectName by rememberSaveable(editing?.id) { mutableStateOf(editing?.subject?.name ?: Subject.PORTUGUES.name) }
    var prompt by rememberSaveable(editing?.id) { mutableStateOf(editing?.prompt ?: "") }
    var optionA by rememberSaveable(editing?.id) { mutableStateOf(editing?.options?.getOrNull(0) ?: "") }
    var optionB by rememberSaveable(editing?.id) { mutableStateOf(editing?.options?.getOrNull(1) ?: "") }
    var optionC by rememberSaveable(editing?.id) { mutableStateOf(editing?.options?.getOrNull(2) ?: "") }
    var optionD by rememberSaveable(editing?.id) { mutableStateOf(editing?.options?.getOrNull(3) ?: "") }
    var correctIndex by rememberSaveable(editing?.id) { mutableIntStateOf(editing?.correctIndex ?: -1) }
    var explanation by rememberSaveable(editing?.id) { mutableStateOf(editing?.explanation ?: "") }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val subject = Subject.valueOf(subjectName)
    val options = listOf(optionA, optionB, optionC, optionD)
    val valid = prompt.isNotBlank() && options.all { it.isNotBlank() } && correctIndex in 0..3

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "Nova pergunta" else "Editar pergunta", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()).widthIn(max = 820.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Matéria", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subject.entries.forEach { item ->
                    FilterButton(item.label, item == subject) { subjectName = item.name }
                }
            }
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pergunta") },
                minLines = 3,
                isError = showErrors && prompt.isBlank()
            )
            Spacer(Modifier.height(18.dp))
            Text("Alternativas", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            Text("Preencha as quatro e marque a resposta correta.", modifier = Modifier.fillMaxWidth(), color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            AnswerEditor("A", optionA, correctIndex == 0, showErrors && optionA.isBlank(), onText = { optionA = it }, onCorrect = { correctIndex = 0 })
            AnswerEditor("B", optionB, correctIndex == 1, showErrors && optionB.isBlank(), onText = { optionB = it }, onCorrect = { correctIndex = 1 })
            AnswerEditor("C", optionC, correctIndex == 2, showErrors && optionC.isBlank(), onText = { optionC = it }, onCorrect = { correctIndex = 2 })
            AnswerEditor("D", optionD, correctIndex == 3, showErrors && optionD.isBlank(), onText = { optionD = it }, onCorrect = { correctIndex = 3 })

            if (showErrors && correctIndex !in 0..3) {
                Text("Marque qual alternativa é a correta.", modifier = Modifier.fillMaxWidth(), color = Red, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Explicação após o acerto (opcional)") },
                minLines = 2
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!valid) {
                        showErrors = true
                    } else {
                        val cleanOptions = options.map { it.trim() }
                        onSave(
                            CustomQuestion(
                                id = editing?.id ?: UUID.randomUUID().toString(),
                                subject = subject,
                                prompt = prompt.trim(),
                                options = cleanOptions,
                                correctIndex = correctIndex,
                                explanation = explanation.trim().ifBlank { "Resposta correta: ${cleanOptions[correctIndex]}." }
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (editing == null) "Salvar pergunta" else "Salvar alterações", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun AnswerEditor(
    letter: String,
    text: String,
    isCorrect: Boolean,
    isError: Boolean,
    onText: (String) -> Unit,
    onCorrect: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.weight(1f),
            label = { Text("$letter) Alternativa") },
            singleLine = true,
            isError = isError
        )
        Spacer(Modifier.width(8.dp))
        if (isCorrect) {
            Button(onClick = onCorrect, modifier = Modifier.width(112.dp), shape = RoundedCornerShape(14.dp)) { Text("✓ Correta") }
        } else {
            OutlinedButton(onClick = onCorrect, modifier = Modifier.width(112.dp), shape = RoundedCornerShape(14.dp)) { Text("Correta?") }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialInputScreen(
    initialSubject: Subject,
    initialTitle: String,
    initialText: String,
    onBack: () -> Unit,
    onGenerate: (Subject, String, String, List<Question>) -> Unit
) {
    var subjectName by rememberSaveable { mutableStateOf(initialSubject.name) }
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var text by rememberSaveable { mutableStateOf(initialText) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var voiceError by rememberSaveable { mutableStateOf<String?>(null) }
    var fileStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var importedFiles by rememberSaveable { mutableStateOf("") }
    var importingFiles by rememberSaveable { mutableStateOf(false) }
    var generatingActivity by rememberSaveable { mutableStateOf(false) }
    var selectedPptUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPptName by rememberSaveable { mutableStateOf<String?>(null) }
    var sourceType by rememberSaveable { mutableStateOf("text") }
    val subject = runCatching { Subject.valueOf(subjectName) }.getOrDefault(Subject.PORTUGUES)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val backendRepository = remember { BackendActivityRepository() }

    suspend fun generateActivity(
        activityTitle: String,
        internalText: String,
        internalSourceType: String,
        pptUri: Uri? = null,
        pptName: String? = null,
        preserveSourceInput: Boolean = false
    ) {
        generatingActivity = true
        error = null
        fileStatus = "Analisando o material e preparando a atividade…"
        try {
            val questions = withContext(Dispatchers.IO) {
                when {
                    pptUri != null -> backendRepository.fromPpt(context, pptUri, pptName ?: "material.ppt", subject.label)
                    Regex("^https?://(?:www\\.)?(?:youtube\\.com|youtu\\.be)/", RegexOption.IGNORE_CASE).containsMatchIn(internalText) ->
                        backendRepository.fromYoutube(internalText, subject.label)
                    else -> backendRepository.fromText(internalSourceType, activityTitle, subject.label, internalText)
                }
            }
            if (questions.isEmpty()) throw BackendException(code = "incompatible_response", message = "No questions returned.")
            onGenerate(
                subject,
                activityTitle,
                com.estudenoah.app.material.MaterialRouting.sourceTextForPersistence(preserveSourceInput, internalText),
                questions
            )
        } catch (failure: BackendException) {
            error = failure.userMessage()
            fileStatus = null
        } catch (_: Exception) {
            error = "Não foi possível preparar a atividade. Tente novamente."
            fileStatus = null
        } finally {
            generatingActivity = false
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (spoken.isNotBlank()) {
                text = spoken.take(25000)
                importedFiles = "Entrada de voz"
                error = null
                voiceError = null
                scope.launch {
                    generateActivity(
                        activityTitle = title.trim().ifBlank { "Material de ${subject.label}" },
                        internalText = text,
                        internalSourceType = "voice"
                    )
                }
            } else {
                voiceError = "Não consegui reconhecer o que foi falado. Tente novamente."
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        importingFiles = true
        fileStatus = "Lendo ${uris.size} arquivo(s)..."
        scope.launch {
            val selected = withContext(Dispatchers.IO) {
                uris.take(8).map { uri ->
                    Triple(uri, MaterialFileExtractor.displayName(context, uri), context.contentResolver.getType(uri))
                }
            }
            val legacyPpts = selected.filter { (_, name, mimeType) ->
                com.estudenoah.app.material.MaterialRouting.route(name, mimeType) ==
                    com.estudenoah.app.material.MaterialRoute.LEGACY_PPT_BACKEND
            }
            if (legacyPpts.isNotEmpty()) {
                if (selected.size != 1) {
                    error = "Para analisar PowerPoint antigo, selecione apenas um arquivo .ppt ou .pps por vez."
                    importingFiles = false
                    return@launch
                }
                selectedPptUri = legacyPpts.first().first.toString()
                selectedPptName = legacyPpts.first().second
                sourceType = "ppt"
                text = ""
                importedFiles = legacyPpts.first().second
                fileStatus = "• ${legacyPpts.first().second}: o arquivo será analisado com segurança pelo servidor."
                if (title.isBlank()) title = legacyPpts.first().second.substringBeforeLast('.').take(80)
                error = null
                importingFiles = false
                generateActivity(
                    activityTitle = title.ifBlank { legacyPpts.first().second.substringBeforeLast('.').take(80) },
                    internalText = "",
                    internalSourceType = "ppt",
                    pptUri = legacyPpts.first().first,
                    pptName = legacyPpts.first().second
                )
                return@launch
            }
            selectedPptUri = null
            selectedPptName = null
            text = ""
            val results = withContext(Dispatchers.IO) { selected.map { (uri, _, _) -> MaterialFileExtractor.extract(context, uri) } }
            val usableTexts = results.mapNotNull { result ->
                result.extractedText.takeIf { it.isNotBlank() }
                    ?.let { "[${result.fileName}]\n$it" }
            }
            if (usableTexts.isNotEmpty()) {
                text = usableTexts.joinToString("\n\n").take(25000)
                error = null
                if (title.isBlank() && results.size == 1) {
                    title = results.first().fileName.substringBeforeLast('.').take(80)
                }
            }
            importedFiles = results.joinToString(" • ") { it.fileName }
            sourceType = results.singleOrNull()?.extension?.ifBlank { "text" } ?: "text"
            fileStatus = if (text.length >= 140) "Analisando o material e preparando a atividade…" else
                results.joinToString("\n") { "• ${it.fileName}: ${com.estudenoah.app.material.MaterialRouting.FRIENDLY_PROCESSING_FAILURE}" }
            importingFiles = false
            if (text.length >= 140) {
                generateActivity(
                    activityTitle = title.trim().ifBlank { "Material de ${subject.label}" },
                    internalText = text,
                    internalSourceType = sourceType
                )
            }
        }
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Novo material", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Pais") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()).widthIn(max = 900.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BlueSoft), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Adicionar material estudado", color = Blue, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Você pode colar texto, ditar por voz ou selecionar arquivos do tablet.", color = Muted)
                }
            }
            Spacer(Modifier.height(18.dp))

            Text("Matéria", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subject.entries.forEach { item ->
                    FilterButton(item.label, item == subject) {
                        subjectName = item.name
                        error = null
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80); error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título do material (opcional)") },
                supportingText = { Text("Ex.: Sistema Solar, Brasil Colônia, Substantivos") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                enabled = !importingFiles,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (importingFiles) "Lendo arquivos..." else "📎 Adicionar PDF, Office, ODT, áudio ou vídeo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Formatos aceitos: PDF, PPT, PPS, PPTX, DOC, DOCX, ODT, MP3, MP4 e AVI. Você pode selecionar mais de um arquivo.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (importedFiles.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Materiais selecionados", color = Green, fontWeight = FontWeight.Bold)
                        Text(importedFiles, color = Ink, fontSize = 13.sp)
                    }
                }
            }
            if (fileStatus != null) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(fileStatus!!, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            if (importedFiles.isBlank()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { value ->
                        text = value.take(25000)
                        error = null
                        voiceError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cole um texto ou uma URL do YouTube") },
                    minLines = 8,
                    supportingText = { Text("${text.length}/25.000 caracteres") },
                    isError = error != null
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    text = ""
                    selectedPptUri = null
                    selectedPptName = null
                    sourceType = "text"
                    importedFiles = ""
                    fileStatus = null
                    error = null
                    voiceError = null
                },
                enabled = text.isNotBlank() || importedFiles.isNotBlank() || fileStatus != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("🗑️ Remover material selecionado", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o conteúdo do material")
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    try {
                        speechLauncher.launch(voiceIntent)
                    } catch (_: ActivityNotFoundException) {
                        voiceError = "Este tablet não encontrou um serviço de reconhecimento de voz disponível."
                    }
                },
                enabled = importedFiles.isBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("🎤 Falar e adicionar ao texto", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "A extração, transcrição, OCR e análise são etapas internas. O aplicativo mostra somente o material selecionado e a atividade pronta.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (voiceError != null) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = RedSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(voiceError!!, color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                }
            }

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = RedSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(error!!, color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                }
            }

            if (generatingActivity) {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = BlueSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Analisando o material e preparando a atividade…", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cleanText = text.trim()
                    val cleanTitle = title.trim().ifBlank { "Material de ${subject.label}" }
                    scope.launch {
                        generateActivity(
                            activityTitle = cleanTitle,
                            internalText = cleanText,
                            internalSourceType = sourceType,
                            pptUri = selectedPptUri?.let(Uri::parse),
                            pptName = selectedPptName,
                            preserveSourceInput = importedFiles.isBlank()
                        )
                    }
                },
                enabled = !generatingActivity && !importingFiles && (selectedPptUri != null || text.trim().length >= 140 || Regex("^https?://(?:www\\.)?(?:youtube\\.com|youtu\\.be)/", RegexOption.IGNORE_CASE).containsMatchIn(text.trim())),
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    if (generatingActivity) "Preparando atividade…" else if (subject == Subject.MATEMATICA) "Gerar atividade de Matemática" else "Gerar atividade pedagógica",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (subject == Subject.MATEMATICA)
                    "Em Matemática, o aplicativo identifica o assunto e cria exercícios matemáticos relacionados ao material."
                else
                    "Nas demais matérias, o aplicativo cria 5 afirmações sobre o material para Noah responder Verdadeiro ou Falso.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialPreviewScreen(
    subject: Subject,
    title: String,
    questions: List<Question>,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Revisar atividade", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Texto") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()).widthIn(max = 900.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(subject.label, color = Green, fontWeight = FontWeight.Bold)
                    Text(title, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (subject == Subject.MATEMATICA)
                            "${questions.size} questões de Matemática geradas a partir do material"
                        else
                            "${questions.size} afirmações de Verdadeiro/Falso geradas a partir do material",
                        color = Muted
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            questions.forEachIndexed { index, question ->
                GeneratedQuestionPreview(index + 1, question)
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onSave,
                enabled = questions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Salvar como atividade preparada", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onTest, enabled = questions.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Testar atividade agora", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onRegenerate) { Text("Gerar outra versão") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ao salvar, esta atividade ficará destacada na tela inicial para Noah. Se já houver outra atividade preparada, ela será substituída.",
                color = Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun GeneratedQuestionPreview(number: Int, question: Question) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            val trueFalse = question.options == listOf("Verdadeiro", "Falso")
            Text(if (trueFalse) "Afirmação $number" else "Questão $number", color = Blue, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(question.prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            question.options.forEachIndexed { index, option ->
                Text(
                    "${('A'.code + index).toChar()}) $option",
                    color = if (index == question.correctIndex) Green else Muted,
                    fontWeight = if (index == question.correctIndex) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (trueFalse) "Classificação correta destacada em verde." else "Resposta correta destacada em verde.",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinScreen(currentPin: String, onBack: () -> Unit, onSave: (String) -> Unit) {
    var oldPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun clean(value: String): String = value.filter { it.isDigit() }.take(8)

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Alterar PIN", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("O PIN deve ter entre 4 e 8 números.", color = Muted)
                Spacer(Modifier.height(16.dp))
                PinField("PIN atual", oldPin) { oldPin = clean(it); error = null }
                Spacer(Modifier.height(10.dp))
                PinField("Novo PIN", newPin) { newPin = clean(it); error = null }
                Spacer(Modifier.height(10.dp))
                PinField("Confirmar novo PIN", confirmPin) { confirmPin = clean(it); error = null }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error!!, color = Red, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = {
                        error = when {
                            oldPin != currentPin -> "O PIN atual está incorreto."
                            newPin.length !in 4..8 -> "O novo PIN precisa ter de 4 a 8 números."
                            newPin != confirmPin -> "A confirmação não corresponde ao novo PIN."
                            newPin == currentPin -> "Escolha um PIN diferente do atual."
                            else -> null
                        }
                        if (error == null) onSave(newPin)
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Salvar novo PIN", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

