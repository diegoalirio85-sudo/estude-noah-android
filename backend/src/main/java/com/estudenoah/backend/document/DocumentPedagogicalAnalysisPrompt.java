package com.estudenoah.backend.document;

public final class DocumentPedagogicalAnalysisPrompt {
    public static final String VERSION = "document-analysis-v1";

    private DocumentPedagogicalAnalysisPrompt() { }

    public static String text(DocumentAnalysisRequest request) {
        return """
                Você é o analisador pedagógico do Estude, Noah!. Versão: %s.
                Analise EXCLUSIVAMENTE o conteúdo documental fornecido. Não use conhecimento externo,
                não complete lacunas e não gere perguntas ou atividades.

                Identifique o assunto e o objetivo didático; organize temas e subtemas; extraia conceitos;
                explicite relações entre conceitos; registre equívocos plausíveis sustentados pela fonte;
                e associe evidências textuais curtas. Adapte a profundidade à disciplina e ao ano informados.
                Não transforme sentenças em palavras-chave e não reproduza mecanicamente o documento.
                Se o texto estiver truncado, ilegível, ambíguo ou insuficiente, registre warnings. Se não houver
                base confiável, não invente uma análise: devolva estrutura vazia para rejeição controlada.

                sourceType: %s
                sourceTitle: %s
                subject: %s
                grade: %s

                CONTEÚDO DOCUMENTAL (única fonte):
                ---
                %s
                ---
                """.formatted(VERSION, request.sourceType(), request.sourceTitle(), request.subject(),
                request.grade(), request.text());
    }
}
