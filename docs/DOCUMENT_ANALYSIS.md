# Análise pedagógica de documentos

## Fluxos D1A

Documentos cujo texto já é extraído com segurança no Android seguem:

`PDF/PPTX/DOCX/ODT/DOC → extração local → texto → backend → análise pedagógica → C2.1 → atividade`

O PowerPoint binário legado é a exceção:

`.ppt → upload efêmero → LegacyPptExtractor/HSLF → texto → análise pedagógica → C2.1 → atividade`

A D1A não conecta o Android. Essa integração pertence à D1B.

## Endpoints

### `POST /v1/materials/text/analyze`

Recebe JSON com `sourceType`, `sourceTitle`, `subject`, `grade` e `text`. São aceitos `pdf`, `ppt`, `pptx`, `doc`, `docx`, `odt` e `text`.

A resposta contém fonte, assunto, resumo, temas, objetivos, conceitos, relações, equívocos plausíveis, evidências e warnings. Esta etapa não gera questões.

### `POST /v1/activities/from-text`

Recebe o mesmo JSON, executa a análise documental e delega a geração ao serviço C2.1 existente. Assim, diversidade cognitiva, validação determinística, retry pedagógico, limite de literalidade e regras de Matemática permanecem centralizados.

### `POST /v1/activities/from-ppt`

Recebe multipart com `file`, `subject` e `grade`. O arquivo precisa terminar em `.ppt`. O endpoint reutiliza `LegacyPptExtractor`; PPT inválido, corrompido, protegido ou sem texto suficiente falha de forma controlada.

## Fidelidade e Structured Output

O prompt versionado `document-analysis-v1` exige uso exclusivo do texto enviado, proíbe conhecimento externo e geração de perguntas e orienta warnings para extração duvidosa. A resposta Gemini usa `document-analysis-schema.json`; metadados de fonte, título e disciplina são impostos pelo servidor, não aceitos do eco do modelo.

Evidências documentais usam o campo compatível `timestamp` com o marcador `document` quando não existe localização de página. Uma evolução futura poderá adotar localizadores por página/bloco sem romper o contrato C2.1.

## Limites e chunking futuro

- mínimo confiável: 140 caracteres úteis;
- máximo por análise: 60.000 caracteres;
- conteúdo maior retorna `413 document_text_too_large`;
- nenhum texto é truncado silenciosamente.

Chunking semântico por seção/página, análise parcial e consolidação deverão ser implementados antes de aceitar documentos maiores.

## Segurança e privacidade

O pipeline é stateless: `request → processamento → response`. Não há banco nem armazenamento permanente. O texto escolar integral e a chave Gemini não são registrados. `GEMINI_API_KEY` permanece exclusiva do backend e Structured Output é solicitado com `store=false`.

## E2E futuro

Um workflow manual poderá enviar texto extraído de um PDF artificial ou público para `from-text`, guardar análise/atividade como artifact e executar validações pedagógicas sem incluir material escolar privado ou credenciais.
