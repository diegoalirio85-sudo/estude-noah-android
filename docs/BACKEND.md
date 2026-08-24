# Backend do Estude, Noah!

## Motivação

Processamentos incompatíveis com o Android ou que dependem de credenciais ficam fora do APK. A primeira responsabilidade real é extrair texto de PowerPoint binário `.ppt` com Apache POI HSLF. As próximas etapas poderão analisar vídeos do YouTube e executar o motor pedagógico, sempre sem expor chaves no aplicativo.

O backend é um projeto Maven/JVM independente em `backend/`. Ele não é incluído em `settings.gradle.kts` e não participa do build Android.

## Tecnologia

- Java 21;
- Spring Boot 4.0.7;
- Apache POI HSLF / `poi-scratchpad` 5.5.1;
- Maven;
- container Linux preparado para Google Cloud Run.

Execução local:

```bash
cd backend
mvn spring-boot:run
```

Container:

```bash
docker build -t estude-noah-backend backend
docker run --rm -e PORT=8080 -p 8080:8080 estude-noah-backend
```

## API

### Saúde

`GET /health`

```json
{"status":"ok","service":"estude-noah-backend"}
```

Não depende de Gemini, arquivos ou serviços externos.

### PowerPoint legado

`POST /v1/materials/ppt/extract`, com `multipart/form-data` e campo `file`.

```bash
curl -F "file=@material.ppt;type=application/vnd.ms-powerpoint" \
  http://localhost:8080/v1/materials/ppt/extract
```

A resposta contém nome seguro do arquivo, quantidade de slides, texto estruturado, slides individuais e `usableForGeneration`. O extrator percorre os slides na ordem do HSLF e agrega títulos e caixas de texto disponíveis, sem renderização e sem OCR. `usableForGeneration` exige pelo menos 140 caracteres efetivamente extraídos, em continuidade ao critério conservador do Android.

Erros:

- `400`: upload ausente, vazio ou ilegível;
- `413`: limite configurado excedido;
- `415`: extensão diferente de `.ppt`;
- `422`: PPT inválido, corrompido, protegido ou não suportado;
- `500`: falha inesperada, sem stack trace na resposta.

O multipart tem limite configurável, com padrão de 50 MB, apenas para desenvolvimento e integração inicial. Ele não é a arquitetura definitiva de produção.

## Upload de produção

Para evitar dependência permanente de limites do servidor HTTP:

1. o backend cria uma autorização curta para um objeto temporário;
2. o cliente envia diretamente para um bucket privado do Cloud Storage;
3. o backend processa o objeto;
4. o resultado textual é devolvido;
5. o objeto é excluído imediatamente;
6. uma regra de lifecycle remove resíduos como proteção adicional.

Devem existir autenticação, autorização por usuário, limite de tamanho, validação de tipo real e proteção contra abuso antes de exposição pública.

## Privacidade e segurança

O padrão é `upload → processamento → extração → exclusão`. O serviço desta fase processa o stream multipart e não implementa armazenamento permanente.

Credenciais, tokens, chaves Gemini e JSON de service account não pertencem ao repositório nem ao APK. Em Cloud Run, usar identidade do serviço e Secret Manager. O arquivo `.env.example` contém apenas nomes e valores não secretos.

O acesso de produção exige Firebase App Check no header `X-Firebase-AppCheck`. `/health` permanece público; todos os endpoints de processamento em `/v1` são protegidos. A verificação usa as chaves públicas rotativas do Firebase, valida assinatura RS256, issuer, audience, expiração e App ID opcional. Consulte `docs/DEPLOYMENT.md`.

A limitação de taxa inicial é local por instância e combina App ID, resumo irreversível do token e IP. Ela reduz abuso acidental, mas não substitui uma solução distribuída caso o Cloud Run escale para várias instâncias.

## YouTube e Gemini

`POST /v1/materials/youtube/analyze`, com `application/json`, recebe uma URL pública oficial do YouTube:

```json
{"url":"https://www.youtube.com/watch?v=VIDEO_ID"}
```

São aceitas variantes HTTPS de `youtube.com/watch`, `www.youtube.com/watch`, `m.youtube.com/watch` e `youtu.be`. A URL é validada por allowlist e normalizada antes da chamada externa. Não há resolução genérica de redirecionamentos, acesso a URLs arbitrárias, login, cookies ou integração com AVA.

O backend envia a URL normalizada diretamente como entrada de vídeo para a Gemini Interactions API (`POST /v1beta/interactions`). O provedor analisa áudio e imagem; o serviço não baixa MP4, não extrai MP3 e não depende de legendas. A requisição usa `store=false`, resposta JSON estruturada por schema, timeout de três minutos e somente uma repetição para respostas transitórias.

Resposta:

```json
{
  "sourceType": "youtube",
  "sourceUrl": "https://www.youtube.com/watch?v=...",
  "videoTitle": "...",
  "subject": "...",
  "summary": "...",
  "themes": [{
    "name": "...",
    "learningObjectives": [],
    "concepts": [],
    "relationships": [],
    "likelyMisconceptions": [],
    "evidence": [{"description":"...","timestamp":"00:42"}]
  }],
  "warnings": []
}
```

Configuração exclusiva do backend:

- `GEMINI_API_KEY`: obrigatória para executar a análise; nunca usar no APK ou no repositório;
- `GEMINI_MODEL`: opcional; padrão atual `gemini-3.6-flash`, sempre substituível pelo ambiente.

O prompt pedagógico é versionado em `YoutubePedagogicalAnalysisPrompt`. Ele exige fidelidade exclusiva ao vídeo, evidências temporais, análise multimodal e avisos para incerteza; esta etapa não gera perguntas.

Erros controlados incluem URL inválida (`400`), vídeo inexistente/privado/inacessível (`422`), limite do provedor (`429`), configuração ou indisponibilidade (`503`), timeout (`504`) e autenticação/resposta inválida do provedor (`502`). Respostas não expõem corpo do provedor, stack trace, chave ou credencial.

Consulte `docs/YOUTUBE_ANALYSIS.md` para contrato, segurança, privacidade, testes e limitações.

## Documentos e atividades

`POST /v1/materials/text/analyze` recebe texto extraído de PDF, PPT, PPTX, DOC, DOCX, ODT ou texto simples e produz análise pedagógica estruturada, sem gerar questões. `POST /v1/activities/from-text` encadeia essa análise ao C2.1 existente.

`POST /v1/activities/from-ppt` recebe o arquivo `.ppt`, `subject` e `grade` em multipart, reutiliza o `LegacyPptExtractor` HSLF e envia o texto resultante pelo mesmo pipeline. Nenhuma extração Apache POI foi duplicada.

O texto aceito possui entre 140 e 60.000 caracteres úteis. Acima desse limite a API retorna erro controlado; não há truncamento silencioso. O serviço não persiste uploads, texto, análise ou atividade e não registra o conteúdo escolar integral. Consulte `docs/DOCUMENT_ANALYSIS.md`.

## Geração pedagógica de atividades

`POST /v1/activities/generate` recebe ano escolar, disciplina, fonte e a análise estruturada da C1. O motor usa a integração Gemini existente com Structured Output, prompt versionado `c2-v1` e validação determinística posterior. Disciplinas conceituais recebem cinco itens V/F por tema, com explicação e evidência; Matemática recebe cinco problemas novos por habilidade, com resposta e solução.

O fluxo é stateless e não salva conteúdo ou atividades. Conteúdo insuficiente e saídas inválidas falham de modo controlado, sem retorno parcial. Consulte `docs/ACTIVITY_GENERATION.md`.

## Estratégia futura para o AVA

Links intermediários do AVA Antônio Vieira devem ser resolvidos no backend com lista de destinos permitidos, limite de redirecionamentos e proteção contra SSRF. Login no AVA, captura de agenda e extensão de navegador permanecem fora desta fase.

