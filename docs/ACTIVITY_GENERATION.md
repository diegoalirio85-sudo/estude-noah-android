# Geração pedagógica de atividades (C2)

## Fluxo

`Material → análise estruturada → objetivo/temas/conceitos/relações/equívocos → geração → validação → resposta`

O endpoint `POST /v1/activities/generate` é stateless: não grava atividades, análises ou conteúdo escolar. A C2 recebe a análise pedagógica já produzida pela C1 e usa os mesmos modelos de tema, objetivo, conceito, relação, equívoco e evidência. A estrutura permite que análises futuras de PPT, PPTX, PDF, DOCX e ODT usem o mesmo motor.

## Contrato

Entrada mínima:

```json
{
  "grade": "4º Ano Ensino Fundamental",
  "subject": "Língua Portuguesa",
  "source": {"type": "youtube", "title": "Aula", "url": "https://www.youtube.com/watch?v=..."},
  "analysis": {
    "sourceType": "youtube",
    "sourceUrl": "https://www.youtube.com/watch?v=...",
    "videoTitle": "Aula",
    "subject": "Língua Portuguesa",
    "summary": "...",
    "themes": [{
      "name": "...",
      "learningObjectives": ["..."],
      "concepts": ["..."],
      "relationships": ["..."],
      "likelyMisconceptions": ["..."],
      "evidence": [{"description": "...", "timestamp": "00:10"}]
    }],
    "warnings": []
  }
}
```

Não Matemática responde com `activityType: TRUE_FALSE`; Matemática responde com `activityType: MATH_PROBLEMS`. Cada tema válido contém exatamente cinco itens. A saída inclui `warnings`, inclusive para ambiguidades ou limitações percebidas pelo modelo.

## Prompt e Structured Output

`PedagogicalActivityPrompt` possui versão `c2-v1`. Ele declara a análise recebida como fonte exclusiva, proíbe fatos externos e geração literal, exige erros conceituais plausíveis, adaptação ao ano escolar, explicações/evidências e separação entre Matemática e disciplinas conceituais.

O cliente Gemini já existente envia `store=false`, usa `GEMINI_API_KEY` e `GEMINI_MODEL` e solicita JSON conforme `activity-generation-schema.json`. Não há parsing de texto livre nem uma segunda configuração de credencial. O backend nunca devolve prompt interno, resposta bruta do provedor, chave ou stack trace.

## Validação determinística

Depois do schema, `ActivityValidator` exige:

- tema, objetivo, evidência e dificuldade `easy`, `medium` ou `hard`;
- exatamente cinco questões por tema;
- afirmação, booleano e explicação para V/F;
- distribuição 2/3 ou 3/2 de respostas V/F;
- problema, resposta e passos de solução em Matemática, sem campos V/F;
- ausência de valores vazios;
- ausência de enunciados e explicações idênticos ou muito semelhantes.

A deduplicação normaliza caixa, acentos, pontuação e espaços e aplica similaridade determinística de Jaccard entre palavras, com limiar de 0,86. Não usa embeddings nem armazenamento.

Uma análise sem temas, objetivos, conceitos ou evidências suficientes falha com `422 insufficient_material`. Uma atividade inconsistente não é devolvida parcialmente: falha como `502 invalid_generated_activity`. Warnings não substituem as invariantes mínimas.

## Teste manual futuro

Com `GEMINI_API_KEY` configurada somente no backend, analisar um vídeo público sobre **Figura de linguagem: gradação**, para **Língua Portuguesa — 4º Ano Ensino Fundamental**, e enviar a análise ao endpoint C2. Revisar se as cinco afirmações exigem compreensão, se as falsas são equívocos plausíveis e se explicação/evidência sustentam cada resposta. O vídeo não é fixture nem dependência dos testes automatizados.

## Limitações

- fidelidade factual continua dependendo da qualidade da análise recebida e requer futura revisão dos pais;
- validação textual detecta duplicação lexical, não equivalência semântica profunda;
- não há segunda tentativa pedagógica para corrigir uma saída inválida nesta fase;
- não há Android remoto, AVA, banco, perfil, sincronização ou adaptação histórica.
