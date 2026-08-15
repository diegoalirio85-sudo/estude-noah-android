# Auditoria de Migração — Fase A

## Snapshot analisado

Arquivo recebido: `estude-noah-android-main.zip`.

O cabeçalho do ZIP identifica o snapshot GitHub pelo SHA:

`3e88bdaed5c61622d43d5050ff37965a66d0a254`

Este SHA é o **candidato a baseline pré-migração**. Antes de criar a tag estável no GitHub, confirmar que este estado é exatamente o que gerou o APK v3.3 testado no tablet.

## Estado Android atual

- package / applicationId: `com.estudenoah.app`
- namespace: `com.estudenoah.app`
- versionCode: `6`
- versionName: `3.3`
- minSdk: `24`
- targetSdk: `37`
- compileSdk: `37`
- Java: `17`
- AGP: `9.2.1`
- Kotlin Compose plugin: `2.3.10`
- Gradle configurado no projeto: `9.4.1`

## Arquivo-fonte efetivo

O fonte Android efetivamente compilado é:

`app/src/main/java/com/estudenoah/app/MainActivity.kt`

Ele possui aproximadamente 2.462 linhas e concentra UI, domínio, persistência, importação de arquivos e geração local de atividades.

Existe também um `MainActivity.kt` na raiz, com aproximadamente 980 linhas. Ele **não é o fonte do módulo Android** e está desatualizado em relação ao arquivo dentro de `app/src/main/...`.

## Achado crítico: arquivos duplicados na raiz

O repositório contém cópias na raiz de arquivos que já existem nos locais corretos do módulo Android. Isso cria alto risco de edição acidental e já explica problemas ocorridos durante as atualizações manuais.

Duplicados observados:

- `MainActivity.kt` — cópia antiga/diferente;
- `AndroidManifest.xml` — cópia do manifest do app;
- `app_icon.xml`;
- `backup_rules.xml`;
- `data_extraction_rules.xml`;
- `strings.xml`;
- `styles.xml`;
- `proguard-rules.pro`;
- `debug.keystore` — duplicado do `app/debug.keystore`;
- `gradle-wrapper.properties` — duplicado do arquivo em `gradle/wrapper/`;
- `build-apk.yml` — cópia antiga do workflow real.

O workflow efetivo é:

`.github/workflows/build-apk.yml`

A cópia `build-apk.yml` da raiz está desatualizada e contém a abordagem antiga com `sdkmanager`, que já havia falhado.

### Recomendação

Depois de criar o baseline, fazer um PR de **higienização estrutural** removendo apenas essas cópias redundantes, com CI obrigatoriamente verde antes do merge.

Não remover nada do módulo `app/` nessa etapa.

## Assinatura do APK

A configuração de debug aponta para:

`app/debug.keystore`

com alias `androiddebugkey` e senhas padrão de debug Android.

O arquivo também está duplicado na raiz; os dois binários são idênticos no snapshot.

### Consequência

Preservar `app/debug.keystore` é essencial para manter a capacidade de instalar versões novas por cima da versão já instalada no tablet.

### Risco

Como a chave está versionada no repositório e utiliza credenciais padrão, ela não deve ser tratada como chave de produção. Enquanto o app é de uso privado e o repositório permanece restrito, pode ser mantida temporariamente para preservar a cadeia de atualização.

Migrar futuramente para uma chave de release exige plano específico, pois trocar a assinatura arbitrariamente impede atualização sobre o app existente.

## Persistência local

O app usa `SharedPreferences` com o nome:

`estude_noah_prefs`

Dados identificados:

- `history` — histórico;
- `parent_pin` — PIN dos pais;
- `custom_questions` — perguntas cadastradas;
- `prepared_activity` — atividade preparada.

### Regra para refatoração

A Fase B não pode alterar essas chaves nem seus formatos sem uma migração explícita e testada. Caso contrário, uma atualização pode aparentar “perder” histórico, PIN, perguntas ou atividade preparada.

## Material e geração local

O código atual já contém:

- texto digitado/colado;
- reconhecimento de voz por `RecognizerIntent`;
- seleção múltipla de documentos;
- DOCX/PPTX/ODT por XML/ZIP;
- DOC/PPT por extração experimental;
- PDF com APIs recentes do Android quando disponíveis;
- seleção de MP3/MP4/AVI, ainda sem transcrição;
- geração V/F local para matérias não matemáticas;
- gerador matemático local por heurísticas.

O objeto `MaterialQuestionGenerator` confirma tecnicamente o problema pedagógico observado: a geração atual é baseada em regras, sentenças e vocabulário, não em compreensão semântica profunda do objetivo didático.

## GitHub Actions atual

Workflow efetivo:

`.github/workflows/build-apk.yml`

Resumo:

1. checkout;
2. Java 17;
3. verifica SDK Android do runner;
4. configura Gradle 9.4.1;
5. executa `gradle --no-daemon :app:assembleDebug --stacktrace`;
6. publica `app-debug.apk` como `Estude-Noah-APK`.

Este é o workflow que deve ser preservado até a primeira refatoração estar estabilizada.

## Gradle Wrapper

Existe `gradlew` e `gradle/wrapper/gradle-wrapper.properties`, mas o snapshot não contém `gradle/wrapper/gradle-wrapper.jar`.

Há scripts `preparar_gradle.sh` e `preparar_gradle.ps1` para baixar/verificar esse JAR.

O GitHub Actions atual contorna isso usando a instalação de Gradle feita pela action `gradle/actions/setup-gradle` e executando `gradle` em vez de `./gradlew`.

### Recomendação futura

Em PR separado, normalizar o wrapper para permitir builds reproduzíveis com `./gradlew` em Codex/local/CI. Não misturar essa mudança com a primeira refatoração funcional.

## README

O `README.md` ainda se apresenta como “Android MVP 1” e descreve uma versão muito anterior ao estado real v3.3.

Atualizá-lo é uma tarefa documental segura da Fase A.

## Ordem recomendada de commits/PRs

### PR A1 — documentação somente

Adicionar:

- `AGENTS.md`;
- `PROJECT.md`;
- `ROADMAP.md`;
- `docs/*`.

Nenhuma alteração em código/Gradle/assinatura.

### PR A2 — higiene de duplicados

Remover cópias redundantes da raiz, após revisar uma a uma.

Não remover:

- `build.gradle.kts` da raiz;
- `settings.gradle.kts`;
- `gradle.properties`;
- `gradlew`/`gradlew.bat`;
- `preparar_gradle.*`;
- `README.md`;
- `INSTALAR_SEM_PROGRAMAS_NO_PC.md`;
- pasta `app/`;
- pasta `.github/`;
- pasta `gradle/`.

### PR A3 — README e processo de release

Atualizar documentação operacional, sem alterar app.

### Fase B — refatoração

Só depois da Fase A:

- extrair modelos;
- extrair storage;
- extrair geradores;
- extrair importação de materiais;
- separar telas Compose;
- preservar comportamento e dados.

## Critério para marcar a Fase A como concluída

- baseline identificado e protegido;
- documentação no GitHub;
- duplicados removidos com build verde;
- README atualizado;
- assinatura documentada;
- CI conhecida e estável;
- primeira especificação de refatoração pronta para Codex.
