package com.estudenoah.app.ui.parents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudenoah.app.data.local.LocalPreferencesRepository
import com.estudenoah.app.domain.PreparedActivity
import com.estudenoah.app.domain.Subject
import com.estudenoah.app.network.BackendActivityRepository
import com.estudenoah.app.network.BackendException
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ParentOperationsScreen(
    questionCount: Int,
    onBack: () -> Unit,
    onManageQuestions: () -> Unit,
    onImportMaterial: () -> Unit,
    onChangePin: () -> Unit,
    onBackendAccount: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BackendActivityRepository() }
    val localPreferences = remember(context) { LocalPreferencesRepository(context) }
    var schoolUrl by rememberSaveable { mutableStateOf("") }
    var schoolTitle by rememberSaveable { mutableStateOf("") }
    var schoolSubjectName by rememberSaveable { mutableStateOf(Subject.HISTORIA.name) }
    var resolvingSchoolUrl by rememberSaveable { mutableStateOf(false) }
    var schoolUrlError by rememberSaveable { mutableStateOf<String?>(null) }
    val schoolSubject = runCatching { Subject.valueOf(schoolSubjectName) }.getOrDefault(Subject.HISTORIA)

    Scaffold(topBar = { TopAppBar(title = { Text("Área dos Pais", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Configurações e acompanhamento", fontWeight = FontWeight.Black)
            ParentCard("Perfil de estudo", "Preferências do aluno e rotina de estudos serão configuradas aqui.")
            ParentCard("Integrações escolares", "Links públicos do AVA e de páginas educacionais podem ser resolvidos para o conteúdo final quando o domínio estiver autorizado.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Link da escola", fontWeight = FontWeight.Bold)
                    Text("Cole o link recebido no AVA ou uma página educacional pública. Se ela apontar para um vídeo do YouTube, o Estude, Noah! encontra o vídeo e prepara a atividade automaticamente.")
                    OutlinedTextField(
                        value = schoolUrl,
                        onValueChange = {
                            schoolUrl = it.take(2000)
                            schoolUrlError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL HTTPS do material") },
                        singleLine = true,
                        isError = schoolUrlError != null
                    )
                    OutlinedTextField(
                        value = schoolTitle,
                        onValueChange = { schoolTitle = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título (opcional)") },
                        singleLine = true
                    )
                    Text("Matéria", fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Subject.entries.forEach { item ->
                            if (item == schoolSubject) {
                                Button(onClick = { schoolSubjectName = item.name; schoolUrlError = null }) { Text(item.label) }
                            } else {
                                OutlinedButton(onClick = { schoolSubjectName = item.name; schoolUrlError = null }) { Text(item.label) }
                            }
                        }
                    }
                    if (schoolUrlError != null) {
                        Text(schoolUrlError!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                resolvingSchoolUrl = true
                                schoolUrlError = null
                                try {
                                    val prepared = withContext(Dispatchers.IO) {
                                        repository.fromSchoolUrl(schoolUrl.trim(), schoolSubject.label)
                                    }
                                    localPreferences.savePreparedActivity(
                                        PreparedActivity(
                                            id = UUID.randomUUID().toString(),
                                            title = schoolTitle.trim().ifBlank { prepared.title }.take(80),
                                            subject = schoolSubject,
                                            sourceText = prepared.youtubeUrl,
                                            questions = prepared.questions,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    onBack()
                                } catch (error: BackendException) {
                                    schoolUrlError = error.userMessage()
                                } catch (_: Exception) {
                                    schoolUrlError = "Não foi possível resolver esse link escolar. Tente novamente."
                                } finally {
                                    resolvingSchoolUrl = false
                                }
                            }
                        },
                        enabled = !resolvingSchoolUrl && schoolUrl.trim().startsWith("https://", ignoreCase = true),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (resolvingSchoolUrl) "Localizando vídeo e preparando…" else "Preparar atividade a partir do link")
                    }
                    Text("O app não recebe senha, cookies ou sessão do AVA. Links que dependem de login podem precisar de integração escolar específica em uma etapa futura.")
                }
            }

            ParentCard("Perguntas locais", "$questionCount pergunta${if (questionCount == 1) "" else "s"} personalizada${if (questionCount == 1) "" else "s"} cadastrada${if (questionCount == 1) "" else "s"}.")
            val backendUser = FirebaseAuth.getInstance().currentUser
            ParentCard("Conta do backend", backendUser?.email?.let { "Conectado como $it" } ?: "Desconectado")
            OutlinedButton(onClick = onBackendAccount, modifier = Modifier.fillMaxWidth()) { Text("Conta do backend") }
            OutlinedButton(onClick = onManageQuestions, modifier = Modifier.fillMaxWidth()) { Text("Gerenciar perguntas locais") }
            OutlinedButton(onClick = onImportMaterial, modifier = Modifier.fillMaxWidth()) { Text("Importar material manualmente") }
            OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) { Text("Alterar PIN") }
        }
    }
}

@Composable
private fun ParentCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail) } }
}
