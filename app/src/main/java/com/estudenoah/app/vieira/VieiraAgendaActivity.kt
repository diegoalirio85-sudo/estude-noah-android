package com.estudenoah.app.vieira

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.estudenoah.app.data.local.LocalPreferencesRepository
import org.json.JSONTokener
import java.io.ByteArrayOutputStream

class VieiraAgendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VieiraAgendaScreen(this) }
    }
}

private val VieiraBlue = Color(0xFF1F4E8C)
private val VieiraBackground = Color(0xFFF6F7FB)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VieiraAgendaScreen(context: Context) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(VieiraAgendaSupport.PORTAL_URL) }
    var capturedText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(VieiraAgendaSupport.defaultTitle()) }
    var capturedPlan by remember { mutableStateOf<DailyLessonPlan?>(null) }
    var status by remember { mutableStateOf("Entre no Portal do Aluno e abra o Plano de Aula diário.") }
    val localPreferences = remember { LocalPreferencesRepository(context) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && capturedText.isNotBlank()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(createAgendaPdf(title, capturedText))
                } ?: error("Não foi possível abrir o arquivo escolhido.")
            }.onSuccess {
                status = "PDF salvo com sucesso."
            }.onFailure {
                status = "Não foi possível salvar o PDF: ${it.message ?: "erro desconhecido"}"
            }
        }
    }

    MaterialTheme {
        Scaffold(
            containerColor = VieiraBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Agenda Vieira", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        TextButton(onClick = { (context as? ComponentActivity)?.finish() }) { Text("← Estude, Noah!") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        currentUrl = url.orEmpty()
                                        status = if (VieiraAgendaSupport.isAllowedPortalUrl(currentUrl)) {
                                            "Página do Portal carregada. Abra a agenda/plano de aula e toque em Capturar."
                                        } else {
                                            "Autenticação em andamento. A captura só é habilitada nas páginas do Portal ASAV."
                                        }
                                    }
                                }
                                loadUrl(VieiraPlanDomExtractor.PLAN_URL)
                                webView = this
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(status, color = VieiraBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        Button(
                            onClick = {
                                val view = webView
                                if (view == null || !VieiraAgendaSupport.isAllowedPortalUrl(currentUrl)) {
                                    status = "Abra primeiro uma página do Portal do Aluno (ASAV)."
                                } else {
                                    view.evaluateJavascript(VieiraPlanDomExtractor.EXTRACTION_SCRIPT) { value ->
                                        val decoded = runCatching { JSONTokener(value).nextValue() as? String }.getOrNull().orEmpty()
                                        val plan = DailyLessonPlanJsonCodec.decodePlan(decoded)
                                        if (plan == null || plan.classes.isEmpty()) {
                                            status = "Não encontrei aulas estruturadas. Abra o Plano de Aula, confirme a data e tente novamente."
                                        } else {
                                            capturedPlan = plan
                                            capturedText = plan.toPrintableText()
                                            title = "Plano de Aula Vieira - ${plan.date.toBrazilianDate()}"
                                            localPreferences.saveDailyLessonPlan(plan)
                                            status = "Plano de ${plan.date.toBrazilianDate()} importado: ${plan.classes.size} aulas e ${plan.homeworkClasses.size} atividades."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Capturar página atual", fontWeight = FontWeight.Bold) }

                        if (capturedPlan != null) {
                            OutlinedButton(
                                onClick = { pdfLauncher.launch("${safeFileName(title)}.pdf") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Salvar plano em PDF") }

                            Text(
                                "A senha e a sessão permanecem somente no navegador. A importação salva apenas data, aulas, conteúdos e lições visíveis; não envia dados ao backend nem gera questões automaticamente.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DailyLessonPlan.toPrintableText(): String = buildString {
    appendLine("Plano de Aula - ${date.toBrazilianDate()}")
    orderedClasses.forEach { lesson ->
        appendLine()
        appendLine("Aula ${lesson.lessonNumber ?: "-"} • ${lesson.startTime.orEmpty()}–${lesson.endTime.orEmpty()} • ${lesson.subject.orEmpty()}")
        lesson.classGroup?.let { appendLine("Turma: $it") }
        lesson.lessonType?.let { appendLine("Tipo: $it") }
        lesson.plannedContent?.let { appendLine("Conteúdo previsto: $it") }
        lesson.completedContent?.let { appendLine("Conteúdo realizado: $it") }
        lesson.homework?.let { appendLine("Lição de casa: $it") }
    }
}.trim()

private fun String.toBrazilianDate(): String {
    val parts = split('-')
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else this
}

private fun createAgendaPdf(title: String, text: String): ByteArray {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 42f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(31, 78, 140)
        textSize = 18f
        isFakeBoldText = true
    }
    val lineHeight = 16f
    val maxWidth = pageWidth - (margin * 2)
    val lines = buildList {
        addAll(wrapPdfText(title.ifBlank { VieiraAgendaSupport.defaultTitle() }, titlePaint, maxWidth))
        add("")
        text.lineSequence().forEach { line ->
            if (line.isBlank()) add("") else addAll(wrapPdfText(line, paint, maxWidth))
        }
    }

    var pageNumber = 1
    var index = 0
    while (index < lines.size) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        var y = margin
        var firstLine = true
        while (index < lines.size && y < pageHeight - margin) {
            val line = lines[index]
            val selectedPaint = if (pageNumber == 1 && firstLine) titlePaint else paint
            if (line.isNotBlank()) page.canvas.drawText(line, margin, y, selectedPaint)
            y += if (selectedPaint === titlePaint) 25f else lineHeight
            firstLine = false
            index++
        }
        document.finishPage(page)
        pageNumber++
    }

    val output = ByteArrayOutputStream()
    document.writeTo(output)
    document.close()
    return output.toByteArray()
}

private fun wrapPdfText(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isBlank()) return listOf("")
    val words = text.trim().split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotBlank()) lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    return lines
}

private fun safeFileName(value: String): String = value
    .ifBlank { VieiraAgendaSupport.defaultTitle() }
    .replace(Regex("[^A-Za-zÀ-ÿ0-9._ -]"), "")
    .trim()
    .replace(Regex("\\s+"), "_")
    .take(70)
