package com.estudenoah.app.ui.trophy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estudenoah.app.domain.HistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrophyScreen(history: List<HistoryEntry>, onBack: () -> Unit) {
    val completed = history.size
    val correct = history.sumOf { it.score }
    val total = history.sumOf { it.total }
    Scaffold(topBar = { TopAppBar(title = { Text("Conquistas", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Seu progresso", fontWeight = FontWeight.Black)
            ProgressCard("Constância", "$completed atividade${if (completed == 1) "" else "s"} concluída${if (completed == 1) "" else "s"}")
            ProgressCard("Aprendizado", if (total == 0) "Comece uma atividade para acompanhar sua evolução." else "$correct de $total respostas corretas de primeira")
            ProgressCard("Evolução", "Cada tentativa conta. Continue avançando no seu ritmo.")
            Text("Aqui não há competição: as conquistas mostram apenas o seu próprio caminho.")
        }
    }
}

@Composable
private fun ProgressCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(detail) } }
}
