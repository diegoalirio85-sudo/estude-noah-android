package com.estudenoah.app.network

internal data class BackendRequest(
    val method: String,
    val path: String,
    val contentType: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap()
)

internal data class BackendResponse(
    val status: Int,
    val body: String,
    val headers: Map<String, List<String>> = emptyMap()
)

internal data class BackendGeneratedActivity(
    val subject: String,
    val grade: String,
    val activityType: String,
    val themes: List<BackendTheme>,
    val warnings: List<String>
)

internal data class BackendTheme(val name: String, val questions: List<BackendQuestion>)

internal data class BackendQuestion(
    val statement: String?,
    val answer: Boolean?,
    val explanation: String,
    val problem: String?,
    val mathAnswer: String?,
    val solutionSteps: List<String>,
    val difficulty: String?,
    val theme: String?
)

internal class BackendException(
    val status: Int? = null,
    val code: String? = null,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    fun userMessage(): String = when {
        code == "firebase_login_required" -> "Peça ao responsável para conectar a Conta do backend na Área dos Pais."
        code == "timeout" -> "A análise demorou mais que o esperado. Tente novamente."
        code == "invalid_json" || code == "incompatible_response" -> "O servidor retornou uma resposta incompatível. Tente novamente."
        code == "material_url_login_required" -> "Esse link depende do login do AVA. Por enquanto, abra o recurso no AVA e cole o link público de destino."
        code == "material_url_untrusted_host" -> "Esse site ainda não está autorizado para resolução automática de links escolares."
        code == "youtube_not_found_in_material_url" -> "Não encontrei um vídeo público do YouTube nessa página."
        code == "material_url_unsupported_content" -> "Esse link escolar não aponta para uma página pública compatível nesta etapa."
        else -> when (status) {
        null -> "Sem conexão com o servidor. Verifique a internet e tente novamente."
        400 -> "Não foi possível analisar os dados enviados. Confira o material."
        401 -> "A sessão da Conta do backend expirou. Peça ao responsável para entrar novamente."
        403 -> "Este dispositivo não tem permissão para concluir a solicitação."
        413 -> "O material é grande demais para ser analisado."
        415 -> "Este formato de material não pode ser analisado."
        422 -> "Não foi possível obter conteúdo suficiente deste material."
        429 -> "Há muitas solicitações no momento. Aguarde um pouco e tente novamente."
        in 500..599 -> "O serviço está temporariamente indisponível. Tente novamente."
        else -> "Não foi possível preparar a atividade. Tente novamente."
        }
    }
}
