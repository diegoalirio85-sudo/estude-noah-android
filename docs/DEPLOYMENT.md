# Implantação segura do backend

## Visão geral

O caminho preparado é `Android → Firebase App Check → Cloud Run → backend → Gemini`. O aplicativo nunca recebe `GEMINI_API_KEY`, credencial de serviço ou bearer token fixo.

O Firebase Admin SDK Java 9.10.0 foi avaliado, mas esse artefato não oferece a API de verificação de App Check disponível em outros Admin SDKs. Por isso, o adaptador Java segue o procedimento oficial para linguagens sem suporte direto: obtém o JWKS público rotativo em `https://firebaseappcheck.googleapis.com/v1/jwks`, limita o cache a seis horas e valida assinatura RS256, tipo JWT, issuer, expiração, audience e subject/App ID. A interface `AppCheckTokenVerifier` mantém testes e futura troca por API nativa isolados.

## Endpoints

Público:

- `GET /health`.

Protegidos por `X-Firebase-AppCheck`:

- `POST /v1/materials/ppt/extract`;
- `POST /v1/materials/youtube/analyze`;
- `POST /v1/materials/text/analyze`;
- `POST /v1/activities/generate`;
- `POST /v1/activities/from-text`;
- `POST /v1/activities/from-ppt`.

Token ausente ou inválido recebe `401`; excesso de chamadas recebe `429`. A validação acontece antes do controller, da extração e do Gemini.

## 1. Projeto Google Cloud e Firebase

1. Crie ou selecione um projeto Google Cloud.
2. Ative Firebase nesse mesmo projeto.
3. Anote o número numérico do projeto; ele será `FIREBASE_PROJECT_NUMBER`.
4. Não baixe nem coloque JSON de service account no repositório ou container.

## 2. Registrar o Android

1. Registre o package `com.estudenoah.app` no Firebase Console.
2. Cadastre o SHA-256 do certificado que assina cada variante distribuída.
3. Baixe o `google-services.json` gerado pelo console somente depois de conferir projeto e package.

O `google-services.json` contém identificadores públicos de configuração, não uma chave privada. O arquivo real validado para `comestudenoahapp` e `com.estudenoah.app` é versionado em `app/google-services.json`; credenciais administrativas nunca devem ser incluídas nele.

## 3. App Check e APK fora da Play Store

Para produção, registre Play Integrity no App Check e associe o SHA-256 correto. O App Check aceita distribuição fora da Play Store quando as opções de reconhecimento/licenciamento forem configuradas para esse canal; não exija `PLAY_RECOGNIZED` ou `LICENSED` para um APK exclusivamente sideloaded. Avalie o nível de integridade do dispositivo conforme os aparelhos reais.

Para desenvolvimento local, a variante `debug` usa `DebugAppCheckProviderFactory`; seu token é credencial de teste e nunca deve ser versionado. Para o tablet corporativo sem ADB/Logcat, use o artifact `Estude-Noah-Sideload-APK`: a variante `sideload` usa `PlayIntegrityAppCheckProviderFactory`, a mesma assinatura debug do APK de teste e não inclui `firebase-appcheck-debug`. A variante `release` também usa somente Play Integrity.

Antes do teste sideload, registre no Firebase o SHA-256 de `app/debug.keystore`. Na configuração App Check do aplicativo Android para distribuição exclusivamente fora do Google Play, deixe `PLAY_RECOGNIZED` e `LICENSED` como não obrigatórios e selecione `Device integrity` como integridade mínima. Não é necessário cadastrar debug secret, não há token fixo no APK e o backend permanece sem bypass.

Não habilite enforcement antes de registrar o app, os certificados e os dispositivos de teste. O backend, porém, deve permanecer protegido antes de ser exposto publicamente.

## 4. Configuração Android

O módulo Android aplica o plugin oficial `com.google.gms.google-services` e processa `app/google-services.json`. A configuração preserva o package `com.estudenoah.app`; nenhum fluxo atual chama o backend nesta fase.

`FirebaseAppCheckTokenProvider` prepara a obtenção futura de token. A chamada remota deverá enviar o valor exclusivamente no header `X-Firebase-AppCheck`, nunca na URL ou em logs.

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
  --set-env-vars APP_CHECK_ENABLED=true,FIREBASE_PROJECT_NUMBER=PROJECT_NUMBER,RATE_LIMIT_REQUESTS_PER_MINUTE=30 \
  --set-secrets GEMINI_API_KEY=GEMINI_API_KEY:latest
```

Opcionalmente defina `FIREBASE_APP_IDS` como lista separada por vírgulas para aceitar somente os App IDs registrados. Sem essa allowlist, ainda são exigidos assinatura, issuer e audience do projeto.

Permissões mínimas:

- executar Cloud Run;
- ler a imagem do Artifact Registry;
- acessar somente o segredo Gemini;
- `roles/firebaseappcheck.tokenVerifier` quando recursos de verificação/consumo online forem adotados.

Não use arquivo de service account; o serviço usa a identidade atribuída pelo Cloud Run.

## 7. Limites e logs

- texto documental: 140 a 60.000 caracteres úteis, sem truncamento;
- JSON HTTP: `MAX_JSON_BYTES`, padrão 1 MiB, verificado antecipadamente quando `Content-Length` existe;
- PPT/multipart: `MAX_UPLOAD_SIZE` 50 MiB e request de 51 MiB; `MAX_UPLOAD_BYTES` acompanha o limite externo;
- taxa: `RATE_LIMIT_REQUESTS_PER_MINUTE`, padrão 30 por chave local.

O limitador é em memória e por instância. Escala horizontal não compartilha contadores; Cloud Armor/API Gateway ou armazenamento distribuído é uma evolução futura. Upload chunked sem `Content-Length` continua limitado pelo parser multipart, enquanto JSON chunked depende dos limites do proxy/Cloud Run e deve ser reforçado na borda.

Logs incluem somente request ID, método, endpoint, status, duração e tamanho declarado. Não registram App Check token, chave Gemini, headers completos ou conteúdo escolar.

## 8. Testes de implantação

Saúde pública:

```bash
curl --fail https://SERVICE_URL/health
```

Endpoint protegido sem token deve retornar `401`:

```bash
curl -i -X POST https://SERVICE_URL/v1/materials/text/analyze -H 'Content-Type: application/json' -d '{}'
```

Com um token obtido pelo SDK Android configurado, envie `X-Firebase-AppCheck: TOKEN` e confirme que a requisição prossegue para a validação do payload. Nunca cole o token em issue, log ou artifact.

## 9. Rollback

1. Liste revisões com `gcloud run revisions list`.
2. Direcione 100% do tráfego à última revisão saudável.
3. Mantenha `GEMINI_API_KEY` no Secret Manager; revogue/rotacione somente se houver suspeita de exposição.
4. Não desative App Check como correção permanente. Para diagnóstico controlado, restrinja acesso ao serviço antes de qualquer desativação temporária.

