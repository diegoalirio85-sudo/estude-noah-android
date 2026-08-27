# Integração Android → backend pedagógico

## Arquitetura

O fluxo privado atual é `Android → Firebase Authentication → ID token → Cloud Run → análise pedagógica → C2.1 → atividade`. A URL padrão é `https://estude-noah-backend-rgwyoc2iwa-rj.a.run.app`, centralizada em `BuildConfig.ESTUDE_NOAH_BACKEND_BASE_URL`. Builds de teste podem sobrescrevê-la pela propriedade Gradle ou variável de ambiente `ESTUDE_NOAH_BACKEND_BASE_URL`.

O APK não contém chave Gemini, service account, segredo Firebase Admin ou token fixo.

## Rotas e materiais

- PDF, PPTX, DOC, DOCX, ODT, texto digitado e voz reconhecida: extração/texto no Android e `POST /v1/activities/from-text` com `sourceType`, `sourceTitle`, `subject`, `grade` e `text`.
- PPT/PPS 97–2003: upload multipart para `POST /v1/activities/from-ppt`, campos `file`, `subject` e `grade`; extração HSLF ocorre no backend. A extensão do `DISPLAY_NAME` prevalece sobre MIME ambíguo; quando o provedor oferece apenas o MIME legado e nenhum sufixo, o cliente usa um filename técnico `.ppt` no multipart sem alterar os bytes originais. PPSX permanece fora deste fluxo.
- YouTube manual: `POST /v1/materials/youtube/analyze` com `url`, seguido de `POST /v1/activities/generate` com a análise integral e a fonte YouTube.
- MP3, MP4 e AVI: continuam sem transcrição; nenhum byte é tratado como texto e nenhuma atividade mecânica é criada.

A série temporária centralizada é `4º Ano Ensino Fundamental`.

## Reprodução de YouTube e conta Premium

A reprodução para o aluno é separada da análise pedagógica. Quando uma `PreparedActivity` foi criada a partir de uma URL HTTPS oficial do YouTube, a Home mostra **Assistir vídeo no YouTube**. O Android tenta abrir primeiro o pacote oficial `com.google.android.youtube`; se ele não estiver instalado, usa o resolvedor padrão do sistema como fallback.

O Estude, Noah! não recebe senha, cookie, token OAuth ou acesso à biblioteca da conta Google/YouTube. Os benefícios de uma conta YouTube Premium conectada ao tablet permanecem administrados exclusivamente pelo aplicativo oficial do YouTube. O app apenas entrega a URL pública por `ACTION_VIEW`.

A validação local aceita apenas HTTPS nos hosts `youtube.com`, `www.youtube.com`, `m.youtube.com` e `youtu.be`, rejeitando domínios parecidos, credenciais embutidas e esquemas não web. Essa validação protege a ação de reprodução e não altera o pipeline Gemini já existente.

## Firebase Authentication e retry

Cada endpoint protegido recebe `Authorization: Bearer <Firebase ID token>` obtido imediatamente antes da requisição. O Firebase SDK gerencia a sessão; o aplicativo não salva senha ou token. Em `401` com código `firebase_auth_token_invalid`, há somente uma nova tentativa com `getIdToken(true)`. Outros erros não geram loop.

O responsável conecta uma conta Email/Password criada manualmente no Firebase Console pela opção **Conta do backend**, protegida pelo PIN da Área dos Pais. O backend valida o ID token e exige que o UID esteja em `ALLOWED_FIREBASE_UIDS`. Não existe cadastro público no app.

App Check permanece no código como modo futuro `BACKEND_AUTH_MODE=app_check`, mas não é exigido junto com Firebase Auth. O modo privado atual não depende de Google Play Console, ADB, Logcat ou debug secret.

## Mapeamento e Matemática

`BackendActivityMapper` converte `GeneratedActivity` para `Question`, que permanece compatível com `PreparedActivity`, histórico e SharedPreferences existentes. `TRUE_FALSE` usa as opções Verdadeiro/Falso. `MATH_PROBLEMS` preserva problema, resposta e passos de solução, sem inventar alternativas ou converter a questão para V/F. Tipos de atividade desconhecidos são rejeitados como resposta incompatível.

## Falhas

Erros de conexão, timeout, 400, 401, 403, 413, 415, 422, 429, 5xx e respostas incompatíveis recebem mensagens simples. Stack traces, conteúdo escolar integral, headers e tokens não são exibidos nem registrados.

Se o backend falhar, o aplicativo não chama silenciosamente `MaterialQuestionGenerator`. O gerador legado permanece no código apenas para compatibilidade histórica e não é o caminho da criação manual D1B1.

## Teste real no tablet

1. Instale o APK debug normal por cima da versão atual.
2. Na Área dos Pais, abra **Conta do backend** e entre com a conta criada pelo responsável.
3. Confirme internet e abra a criação manual.
4. Teste texto, um documento extraído, um `.ppt` ou `.pps` isolado e uma URL YouTube.
5. Para YouTube, salve a atividade preparada, volte à Home e toque em **Assistir vídeo no YouTube**; confirme que o app oficial abre na conta esperada e que **Fazer atividade preparada** continua independente.
6. Confirme atividade preparada, execução, resultado e histórico.
7. Simule ausência de rede e confirme que nenhuma atividade local falsa é criada.

CI usa transportes e tokens falsos; não chama Cloud Run, Firebase ou Gemini reais.
