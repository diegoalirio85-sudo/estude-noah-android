package com.estudenoah.backend.activity;

public final class PedagogicalActivityPrompt {
    public static final String VERSION = "c2.1-v1";

    private PedagogicalActivityPrompt() {
    }

    public static String text(ActivityGenerationRequest request, String requestJson) {
        return text(request, requestJson, null);
    }

    public static String text(ActivityGenerationRequest request, String requestJson, String feedback) {
        return """
                Transforme a análise pedagógica fornecida em uma atividade didática em português do Brasil.
                A análise JSON é a fonte EXCLUSIVA. Não use conhecimento geral, não invente fatos, não inclua tema sem evidência e registre incerteza em warnings.

                Respeite o ano escolar e a disciplina informados, adequando vocabulário, abstração, extensão e dificuldade sem infantilizar.

                Para disciplinas não matemáticas, use activityType TRUE_FALSE e gere exatamente 5 afirmações por tema. No máximo 2 podem usar diretamente exemplos da fonte; prefira apenas 1. Pelo menos 2 devem ser application ou relation e pelo menos 1 falsa deve ser misconception quando a análise trouxer likelyMisconceptions. Não copie frases. Não crie falsa por simples negação, troca de palavra-chave, número, ordem, ou inversão crescente/decrescente. Crie situações novas somente quando deriváveis da regra ensinada e inequívocas. Use variedade: 1 compreensão, 2 aplicações, 1 relação/comparação e 1 misconception/análise. Em cada conjunto, use 2/3 ou 3/2 respostas verdadeiras/falsas.

                Para Matemática, use activityType MATH_PROBLEMS e gere exatamente 5 problemas novos por habilidade/tema. Não use verdadeiro/falso. Use números ou contextos diferentes, mas teste a mesma habilidade e não exija informação externa. Inclua resposta e solução passo a passo.

                Todo item deve ter dificuldade easy, medium ou hard: no máximo 1 easy, preferencialmente 3 medium e até 1 hard adequado ao ano. Todo item conceitual deve trazer cognitiveDemand (understanding, application ou analysis) e constructionType (concept, application, relation, misconception ou source_example), além de afirmação, resposta, explicação, evidências, tema e objetivo. Evidence sustenta o conceito; não é texto para copiar.

                Evite duplicatas, paráfrases próximas, explicações idênticas, pistas óbvias e padrões previsíveis. Se a análise não sustentar cinco itens distintos e corretos por tema, não invente conteúdo: explique a insuficiência em warnings.

                Ano escolar: %s
                Disciplina: %s
                Entrada estruturada JSON (inclui fonte e análise): %s
                Feedback objetivo da validação anterior (se houver): %s
                Versão do prompt: %s
                """.formatted(request.grade(), request.subject(), requestJson,
                        feedback == null ? "nenhum; esta é a primeira tentativa" : feedback, VERSION);
    }
}
