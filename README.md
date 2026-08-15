# Estude, Noah! — Android

Aplicativo educacional para tablet Android que transforma atividades preparadas por um responsável em sessões curtas de estudo para o aluno.

O estado atual do aplicativo é a versão **3.3** (`versionCode 6`). O package permanente é `com.estudenoah.app`.

## Funcionalidades atuais

- experiência do aluno otimizada para tablet, em orientação vertical ou horizontal;
- Português, Matemática, Ciências, História e Geografia;
- atividades com uma questão por vez e alternativas grandes;
- nova tentativa após erro;
- pontuação baseada nos acertos da primeira tentativa;
- resultado final e histórico local de até 50 atividades;
- Área dos Pais protegida por PIN;
- cadastro e preparação de perguntas;
- entrada de material por texto e voz;
- seleção e extração de alguns formatos de arquivo;
- ação para apagar apenas o material extraído;
- geração local de atividades, com tratamento específico para Matemática;
- funcionamento local, sem conta ou servidor obrigatório.

A geração local atual é baseada em regras e permanece como comportamento legado. A evolução planejada deve introduzir análise didática antes da geração, conforme [docs/PEDAGOGIA.md](docs/PEDAGOGIA.md) e [docs/GERACAO_DE_ATIVIDADES.md](docs/GERACAO_DE_ATIVIDADES.md).

## Tecnologia

- Kotlin;
- Jetpack Compose;
- Material 3;
- Android Gradle Plugin 9.2.1;
- Kotlin Compose plugin 2.3.10;
- Gradle 9.4.1;
- Java 17;
- compileSdk 37 / targetSdk 37;
- minSdk 24.

## Estrutura efetiva

- módulo Android: `app/`;
- fonte principal: `app/src/main/java/com/estudenoah/app/MainActivity.kt`;
- recursos: `app/src/main/res/`;
- configuração do módulo: `app/build.gradle.kts`;
- configuração raiz: `build.gradle.kts` e `settings.gradle.kts`;
- workflow de build: `.github/workflows/build-apk.yml`;
- assinatura de debug preservada: `app/debug.keystore`.

Não altere o package, a assinatura, as versões ou as chaves de `SharedPreferences` sem um plano explícito de migração e testes de atualização.

## Como abrir no Android Studio

1. Use uma versão atual do Android Studio compatível com AGP 9.2.
2. Abra a raiz deste repositório.
3. Aguarde o Gradle Sync e a instalação do SDK 37, se solicitada.
4. Conecte o tablet Samsung com depuração USB ou use um emulador.
5. Execute a configuração do módulo `app`.

O repositório ainda não contém `gradle/wrapper/gradle-wrapper.jar`. Os scripts `preparar_gradle.ps1` e `preparar_gradle.sh` baixam o arquivo esperado e verificam seu checksum. O GitHub Actions usa uma instalação controlada do Gradle 9.4.1 e não depende desse JAR.

## Como gerar o APK

O caminho preferencial é o GitHub Actions:

1. abra **Actions**;
2. escolha **Gerar APK - Estude Noah**;
3. use **Run workflow** e selecione a branch desejada;
4. aguarde a execução verde;
5. baixe o artefato **Estude-Noah-APK**.

O ZIP do artefato contém `app-debug.apk`. Consulte [INSTALAR_SEM_PROGRAMAS_NO_PC.md](INSTALAR_SEM_PROGRAMAS_NO_PC.md) para instalar no tablet.

No Android Studio, o APK de debug também pode ser gerado em:

`app/build/outputs/apk/debug/app-debug.apk`

## Desenvolvimento seguro

Antes de alterar código, leia [AGENTS.md](AGENTS.md), [PROJECT.md](PROJECT.md) e [ROADMAP.md](ROADMAP.md).

Mudanças relevantes devem seguir:

1. branch dedicada;
2. alteração incremental;
3. compilação e testes;
4. revisão do diff;
5. Pull Request;
6. GitHub Actions verde;
7. merge;
8. teste do APK no tablet quando houver impacto funcional ou de atualização.

A arquitetura alvo e a estratégia de testes estão documentadas em [docs/ARQUITETURA.md](docs/ARQUITETURA.md) e [docs/TESTES.md](docs/TESTES.md). O processo de entrega está em [docs/RELEASES.md](docs/RELEASES.md).
