# Integração Android → backend pedagógico

## Arquitetura

O fluxo é `Android → Firebase App Check → Cloud Run → análise pedagógica → C2.1 → atividade`. A URL padrão é `https://estude-noah-backend-rgwyoc2iwa-rj.a.run.app`, centralizada em `BuildConfig.ESTUDE_NOAH_BACKEND_BASE_URL`. Builds de teste podem sobrescrevê-la pela propriedade Gradle ou variável de ambiente `ESTUDE_NOAH_BACKEND_BASE_URL`.

O APK não contém chave Gemini, service account, segredo Firebase Admin ou token fixo.

## Rotas e materiais

- PDF, PPTX, DOC, DOCX, ODT, texto digitado e voz reconhecida: extração/texto no Android e `POST /v1/activities/from-text` com `sourceType`, `sourceTitle`, `subject`, `grade` e `text`.
- PPT 97–2003: upload multipart para `POST /v1/activities/from-ppt`, campos `file`, `subject` e `grade`; extração HSLF ocorre no backend.
- YouTube manual: `POST /v1/materials/youtube/analyze` com `url`, seguido de `POST /v1/activities/generate` com a análise integral e a fonte YouTube.
- MP3, MP4 e AVI: continuam sem transcrição; nenhum byte é tratado como texto e nenhuma atividade mecânica é criada.

A série temporária centralizada é `4º Ano Ensino Fundamental`.

## App Check e retry

Cada endpoint protegido recebe `X-Firebase-AppCheck` obtido imediatamente antes da requisição. O SDK pode usar seu cache normal; o aplicativo não persiste nem registra o token. Em `401` com código `app_check_token_invalid`, há somente uma nova tentativa com `forceRefresh=true`. Outros erros não geram loop.

Cada variante escolhe explicitamente seu provider App Check:

- `debug`: `DebugAppCheckProviderFactory`, somente para desenvolvimento local com Logcat disponível;
- `sideload`: `PlayIntegrityAppCheckProviderFactory`, para o teste físico fora do Google Play;
- `release`: `PlayIntegrityAppCheckProviderFactory`.

O backend nunca possui bypass. A variante `sideload` não recebe a dependência `firebase-appcheck-debug`, não contém token fixo e mantém o mesmo `applicationId`, URL do backend e assinatura debug do APK de teste existente.

Para teste sideload, registre no Firebase o SHA-256 do certificado de `app/debug.keystore` e configure o App Check/Play Integrity para aceitar distribuição fora do Google Play: `PLAY_RECOGNIZED` não obrigatório, `LICENSED` não obrigatório e integridade mínima `Device integrity`. Não ative enforcement adicional até concluir a validação. Um APK assinado por outro certificado não pode ser atualizado por esta variante sem desinstalação.

## Mapeamento e Matemática

`BackendActivityMapper` converte `GeneratedActivity` para `Question`, que permanece compatível com `PreparedActivity`, histórico e SharedPreferences existentes. `TRUE_FALSE` usa as opções Verdadeiro/Falso. `MATH_PROBLEMS` preserva problema, resposta e passos de solução, sem inventar alternativas ou converter a questão para V/F.

## Falhas

Erros de conexão, timeout, 400, 401, 403, 413, 415, 422, 429, 5xx e respostas incompatíveis recebem mensagens simples. Stack traces, conteúdo escolar integral, headers e tokens não são exibidos nem registrados.

Se o backend falhar, o aplicativo não chama silenciosamente `MaterialQuestionGenerator`. O gerador legado permanece no código apenas para compatibilidade histórica e não é o caminho da criação manual D1B1.

## Teste real no tablet

1. Instale o APK debug.
2. Capture o token App Check Debug emitido pelo SDK e registre-o no Firebase Console.
3. Confirme internet e abra a criação manual.
4. Teste texto, um documento extraído, um `.ppt` isolado e uma URL YouTube.
5. Confirme atividade preparada, execução, resultado e histórico.
6. Simule ausência de rede e confirme que nenhuma atividade local falsa é criada.

CI usa transportes e tokens falsos; não chama Cloud Run, Firebase ou Gemini reais.

