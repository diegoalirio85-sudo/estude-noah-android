# Implantação segura do backend

## Visão geral

O modo privado atual é `Android → Firebase Authentication → ID token → Cloud Run → validação de UID → backend → Gemini`. O aplicativo nunca recebe `GEMINI_API_KEY`, credencial de serviço ou bearer token fixo.

Como modo alternativo futuro, o código ainda preserva suporte a App Check. O Firebase Admin SDK Java 9.10.0 não oferece a mesma API de verificação de App Check disponível em outros Admin SDKs; por isso, o adaptador Java desse modo alternativo obtém o JWKS público rotativo em `https://firebaseappcheck.googleapis.com/v1/jwks`, limita o cache a seis horas e valida assinatura RS256, tipo JWT, issuer, expiração, audience e subject/App ID. A interface `AppCheckTokenVerifier` mantém essa implementação isolada do modo privado atual.

## Endpoints

Público:

- `GET /health`.

Protegidos por `Authorization: Bearer <Firebase ID token>` no modo `firebase_auth`:

- `POST /v1/materials/ppt/extract`;
- `POST /v1/materials/youtube/analyze`;
- `POST /v1/materials/text/analyze`;
- `POST /v1/activities/generate`;
- `POST /v1/activities/from-text`;
- `POST /v1/activities/from-ppt`.

Token ausente ou inválido recebe `401`; UID fora da allowlist recebe `403`; allowlist vazia falha fechada; excesso de chamadas recebe `429`. A validação acontece antes do controller, da extração e do Gemini.

## 1. Projeto Google Cloud e Firebase

1. Crie ou selecione um projeto Google Cloud.
2. Ative Firebase nesse mesmo projeto.
3. Anote o ID do projeto para `FIREBASE_PROJECT_ID`; o número numérico do projeto só é necessário se o modo alternativo `app_check` for adotado.
4. Não baixe nem coloque JSON de service account no repositório ou container.

## 2. Registrar o Android

1. Registre o package `com.estudenoah.app` no Firebase Console.
2. Cadastre o SHA-256 do certificado das variantes que precisarem de recursos Firebase associados ao certificado, especialmente se App Check/Play Integrity voltar a ser usado.
3. Baixe o `google-services.json` gerado pelo console somente depois de conferir projeto e package.

O `google-services.json` contém identificadores públicos de configuração, não uma chave privada. O arquivo real validado para `comestudenoahapp` e `com.estudenoah.app` é versionado em `app/google-services.json`; credenciais administrativas nunca devem ser incluídas nele.

## 3. Firebase Authentication privado

1. No Firebase Console, habilite Authentication → Email/Password.
2. Crie manualmente a conta do responsável; o Android não oferece cadastro público.
3. Copie o UID da conta e configure-o em `ALLOWED_FIREBASE_UIDS` no Cloud Run. Separe múltiplos UIDs por vírgula.
4. Use `BACKEND_AUTH_MODE=firebase_auth` e `FIREBASE_PROJECT_ID=comestudenoahapp`.

O Firebase Admin SDK 9.10.0 valida assinatura, issuer, audience, expiração e UID usando Application Default Credentials da identidade do Cloud Run e o project ID configurado explicitamente. Não use JSON de service account. App Check permanece disponível como modo futuro `app_check`, mas não deve ser exigido simultaneamente no modo privado.

## 4. Configuração Android

O módulo Android aplica o plugin oficial `com.google.gms.google-services` e processa `app/google-services.json`. A configuração preserva o package `com.estudenoah.app`; o fluxo D1B1 chama o backend privado para documentos, PowerPoint legado e YouTube.

`FirebaseAuth` mantém a sessão do responsável. A senha é entregue diretamente ao SDK e nunca é salva pelo app. Cada chamada obtém um ID token e o envia exclusivamente em `Authorization`, nunca na URL ou em logs.

## 5. Secret Manager

1. Crie o segredo `GEMINI_API_KEY` no Secret Manager.
2. Adicione uma versão com o valor real.
3. Conceda à service account do Cloud Run apenas `roles/secretmanager.secretAccessor` nesse segredo.
4. Injete o segredo como variável de ambiente `GEMINI_API_KEY` ao implantar.

Não configure o valor no Dockerfile, YAML versionado ou variável comum do repositório.

## 6. Cloud Run

Construa o container existente com Java 21:

```bash
gcloud builds submit --tag REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/estude-noah-backend backend
```

Implante usando identidade de workload e a porta fornecida pelo Cloud Run:

```bash
gcloud run deploy estude-noah-backend \
  --image REGION-docker.pkg.dev/PROJECT_ID/REPOSITORY/estude-noah-backend \
  --region REGION \
  --set-env-vars BACKEND_AUTH_MODE=firebase_auth,FIREBASE_PROJECT_ID=comestudenoahapp,ALLOWED_FIREBASE_UIDS=UID_AUTORIZADO,RATE_LIMIT_REQUESTS_PER_MINUTE=30 \
  --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest
```

`FIREBASE_PROJECT_ID` e `ALLOWED_FIREBASE_UIDS` são obrigatórios no modo atual. O project ID é aplicado explicitamente ao `FirebaseOptions`; não se depende de metadata do Cloud Run para descobri-lo. Se qualquer configuração obrigatória estiver ausente, os endpoints protegidos falham de forma segura.

Permissões mínimas da identidade do serviço devem seguir o princípio do menor privilégio. Para este backend, mantenha apenas as permissões efetivamente exigidas pelos recursos usados, incluindo acesso ao segredo Gemini e às operações de infraestrutura necessárias ao ambiente. Não use arquivo de service account; o serviço usa a identidade atribuída pelo Cloud Run.

## 7. Limites e logs

- texto documental: 140 a 60.000 caracteres úteis, sem truncamento;
- JSON HTTP: `MAX_JSON_BYTES`, padrão 1 MiB, verificado antecipadamente quando `Content-Length` existe;
- PPT/multipart: `MAX_UPLOAD_SIZE` 50 MiB e request de 51 MiB; `MAX_UPLOAD_BYTES` acompanha o limite externo;
- taxa: `RATE_LIMIT_REQUESTS_PER_MINUTE`, padrão 30 por chave local.

O limitador é em memória e por instância. Escala horizontal não compartilha contadores; Cloud Armor/API Gateway ou armazenamento distribuído é uma evolução futura. Upload chunked sem `Content-Length` continua limitado pelo parser multipart, enquanto JSON chunked depende dos limites do proxy/Cloud Run e deve ser reforçado na borda.

Logs incluem somente request ID, método, endpoint, status, duração e tamanho declarado. Não registram Firebase ID token, chave Gemini, headers completos ou conteúdo escolar.

## 8. Testes de implantação

Saúde pública:

```bash
curl --fail https://SERVICE_URL/health
```

Endpoint protegido sem token deve retornar `401`:

```bash
curl -i -X POST https://SERVICE_URL/v1/materials/text/analyze -H 'Content-Type: application/json' -d '{}'
```

Depois de conectar a conta na Área dos Pais, confirme pelo Android que uma atividade chega ao backend. Nunca copie o ID token para issue, log ou artifact.

## 9. Rollback

1. Liste revisões com `gcloud run revisions list`.
2. Direcione 100% do tráfego à última revisão saudável.
3. Mantenha `GEMINI_API_KEY` no Secret Manager; revogue/rotacione somente se houver suspeita de exposição.
4. `BACKEND_AUTH_MODE=none` é bypass de teste e não deve ser usado em serviço acessível; para diagnóstico, use ambiente isolado sem tráfego real.
