package com.estudenoah.backend.activity;

public final class PedagogicalActivityPrompt {
    public static final String VERSION = "c2-v1";

    private PedagogicalActivityPrompt() {
    }

    public static String text(ActivityGenerationRequest request, String requestJson) {
        return """
                Transforme a análise pedagógica fornecida em uma atividade didática em português do Brasil.
                A análise JSON é a fonte EXCLUSIVA. Não use conhecimento geral, não invente fatos, não inclua tema sem evidência e registre incerteza em warnings.

                Respeite o ano escolar e a disciplina informados, adequando vocabulário, abstração, extensão e dificuldade sem infantilizar.

                Para disciplinas não matemáticas, use activityType TRUE_FALSE e gere exatamente 5 afirmações por tema. Não copie, recorte, negue ou altere palavras/datas de frases da fonte. Construa itens por compreensão de conceitos, relações, causa e consequência, comparação, função, aplicação ou inferência. Afirmações falsas devem representar equívocos plausíveis, nunca absurdos. Em cada conjunto, use 2/3 ou 3/2 respostas verdadeiras/falsas em ordem não previsível.

                Para Matemática, use activityType MATH_PROBLEMS e gere exatamente 5 problemas novos por habilidade/tema. Não use verdadeiro/falso. Use números ou contextos diferentes, mas teste a mesma habilidade e não exija informação externa. Inclua resposta e solução passo a passo.

                Todo item deve ter dificuldade easy, medium ou hard, privilegiando medium. Todo item conceitual deve trazer afirmação, resposta booleana, explicação, evidências, tema e objetivo didático. Todo item matemático deve trazer problema, mathAnswer, solutionSteps, skill, tema, objetivo didático, dificuldade e evidências.

                Evite duplicatas, paráfrases próximas, explicações idênticas, pistas óbvias e padrões previsíveis. Se a análise não sustentar cinco itens distintos e corretos por tema, não invente conteúdo: explique a insuficiência em warnings.

                Ano escolar: %s
                Disciplina: %s
                Entrada estruturada JSON (inclui fonte e análise): %s
                Versão do prompt: %s
                """.formatted(request.grade(), request.subject(), requestJson, VERSION);
    }
}
