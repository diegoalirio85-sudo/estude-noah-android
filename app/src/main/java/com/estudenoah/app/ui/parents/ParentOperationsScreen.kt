package com.estudenoah.app.ui.parents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

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
    Scaffold(topBar = { TopAppBar(title = { Text("Área dos Pais", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Configurações e acompanhamento", fontWeight = FontWeight.Black)
            ParentCard("Perfil de estudo", "Preferências do aluno e rotina de estudos serão configuradas aqui.")
            ParentCard("Integrações escolares", "Agenda, AVA e materiais sincronizados serão conectados em etapas futuras.")
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

