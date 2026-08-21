package com.estudenoah.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
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
internal fun MaterialDetailScreen(material: TodayMaterial, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Material") }, navigationIcon = { TextButton(onClick = onBack) { Text("← Início") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(material.type, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(material.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(material.source)
            Text("Prévia demonstrativa. A sincronização de materiais será conectada em uma fase posterior.")
        }
    }
}
