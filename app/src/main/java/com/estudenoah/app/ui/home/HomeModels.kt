package com.estudenoah.app.ui.home

internal data class TodayAgendaItem(
    val time: String,
    val title: String,
    val detail: String
)

internal data class TodayMaterial(
    val id: String,
    val type: String,
    val title: String,
    val source: String
)

internal data class TodayActivity(
    val subject: String,
    val title: String,
    val status: String
)

internal object HomePreviewData {
    val agenda = emptyList<TodayAgendaItem>()

    val materials = listOf(
        TodayMaterial("material-1", "PDF", "Leitura do dia", "Material demonstrativo"),
        TodayMaterial("material-2", "YouTube", "Vídeo da aula", "Integração em breve")
    )

    val activities = listOf(
        TodayActivity("Língua Portuguesa", "Interpretação e vocabulário", "Disponível"),
        TodayActivity("Matemática", "Desafio rápido", "Planejada")
    )
}
