package com.estudenoah.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewScreen(onBack: () -> Unit, onStart: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Revisar", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Escolha o que deseja revisar", fontWeight = FontWeight.Black)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Matéria", fontWeight = FontWeight.Bold); Text("Todas as matérias (seleção em breve)") } }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("Período", fontWeight = FontWeight.Bold); Text("Atividades recentes (filtro em breve)") } }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Começar revisão") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
}
