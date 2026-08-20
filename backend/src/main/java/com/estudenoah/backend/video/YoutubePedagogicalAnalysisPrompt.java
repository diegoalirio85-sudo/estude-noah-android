package com.estudenoah.backend.video;

public final class YoutubePedagogicalAnalysisPrompt {
    public static final String VERSION = "c1-v1";

    private YoutubePedagogicalAnalysisPrompt() {
    }

    public static String text() {
        return """
                Analise este vídeo público do YouTube como material didático. Use conjuntamente o áudio falado e os elementos visuais efetivamente observáveis (textos na tela, diagramas, demonstrações e imagens). Responda em português do Brasil.

                A fonte exclusiva de cada conclusão é o próprio vídeo. Não use conhecimento externo, não complete lacunas, não invente fatos e não crie perguntas ou atividades. Se áudio, imagem, título ou contexto forem ambíguos, registre a limitação em warnings e reduza a certeza da análise.

                Identifique título, matéria, resumo, temas, objetivos de aprendizagem, conceitos, relações entre conceitos e equívocos plausíveis que o material permite antecipar. Para cada tema, forneça evidências específicas com descrição e timestamp no formato MM:SS ou HH:MM:SS. Evidências devem apontar o trecho audiovisual que sustenta a análise, sem copiar longos trechos falados.

                No JSON, use sourceType exatamente "youtube" e copie em sourceUrl exatamente a URL canônica recebida como entrada de vídeo.

                Preserve fidelidade pedagógica: analise compreensão, causa/consequência, comparação, função, aplicação e inferência somente quando sustentadas pelo vídeo. Não gere questões nesta etapa.

                Versão do prompt: %s
                """.formatted(VERSION);
    }
}
