package com.estudenoah.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.estudenoah.app.domain.HistoryEntry
import com.estudenoah.app.domain.PreparedActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF3F63C8)
private val BlueSoft = Color(0xFFE9EEFF)
private val Green = Color(0xFF2E7D5B)
private val GreenSoft = Color(0xFFE7F6EE)
private val Cream = Color(0xFFFFF8F0)
private val Muted = Color(0xFF65708A)

@Composable
internal fun FiveZoneHomeScreen(
    history: List<HistoryEntry>,
    preparedActivity: PreparedActivity?,
    onCreateActivity: () -> Unit,
    onQuickPractice: () -> Unit,
    onReview: () -> Unit,
    onTrophies: () -> Unit,
    onParents: () -> Unit,
    onHistory: () -> Unit,
    onPrepared: (PreparedActivity) -> Unit,
    onMaterial: (TodayMaterial) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HomeHeader() }
            item {
                ResponsivePair(wide,
                    first = { modifier -> ZoneCard("Agenda de hoje", "Organize o seu dia", modifier) {
                        if (HomePreviewData.agenda.isEmpty()) {
                            Text("Nenhum compromisso para hoje.", color = Muted)
                            Text("A agenda escolar será conectada em uma próxima etapa.", color = Muted)
                        }
                    } },
                    second = { modifier -> ZoneCard("Materiais de hoje", "Conteúdos separados para estudar", modifier) {
                        HomePreviewData.materials.forEach { material ->
                            CompactItem(
                                eyebrow = material.type,
                                title = material.title,
                                detail = material.source,
                                onClick = { onMaterial(material) }
                            )
                        }
                    } })
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueSoft)
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Central de estudos", color = Blue, fontWeight = FontWeight.Bold)
                        Text("O que vamos aprender agora?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        preparedActivity?.let {
                            Card(colors = CardDefaults.cardColors(containerColor = GreenSoft)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("Atividade preparada", color = Green, fontWeight = FontWeight.Bold)
                                    Text(it.title, fontWeight = FontWeight.Bold)
                                    Button(onClick = { onPrepared(it) }, modifier = Modifier.fillMaxWidth()) { Text("Fazer atividade preparada") }
                                }
                            }
                        }
                        Button(onClick = onCreateActivity, modifier = Modifier.fillMaxWidth().height(58.dp)) { Text("Criar nova atividade", fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = onQuickPractice, modifier = Modifier.fillMaxWidth()) { Text("Praticar agora") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeShortcut("Revisar", onReview, Modifier.weight(1f))
                            HomeShortcut("Conquistas", onTrophies, Modifier.weight(1f))
                            HomeShortcut("Área dos Pais", onParents, Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                ResponsivePair(wide,
                    first = { modifier -> ZoneCard("Atividades de hoje", "Sugestões prontas para continuar", modifier) {
                        HomePreviewData.activities.forEach { activity ->
                            CompactItem(activity.subject, activity.title, activity.status, onQuickPractice)
                        }
                    } },
                    second = { modifier -> ZoneCard("Últimas 10 atividades", "Seu caminho mais recente", modifier) {
                        if (history.isEmpty()) Text("As atividades concluídas aparecerão aqui.", color = Muted)
                        history.take(10).forEach { entry ->
                            CompactItem(entry.subject, "${entry.score}/${entry.total} acertos de primeira", formatShortDate(entry.timestamp), onHistory)
                        }
                        if (history.isNotEmpty()) OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Ver histórico completo") }
                    } })
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(Modifier.fillMaxWidth()) {
        Text("Estude, Noah!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(friendlyDate(), color = Muted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResponsivePair(
    wide: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    if (wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        first(Modifier.fillMaxWidth())
        second(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ZoneCard(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = Muted)
            content()
        }
    }
}

@Composable
private fun CompactItem(eyebrow: String, title: String, detail: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Cream)) {
        Column(Modifier.padding(12.dp)) {
            Text(eyebrow, color = Blue, fontWeight = FontWeight.Bold)
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted)
        }
    }
}

@Composable
private fun HomeShortcut(label: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(54.dp)) { Text(label, textAlign = TextAlign.Center) }
}

private fun friendlyDate(): String = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
    .format(Date()).replaceFirstChar { it.titlecase(Locale("pt", "BR")) }

private fun formatShortDate(timestamp: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(timestamp))
