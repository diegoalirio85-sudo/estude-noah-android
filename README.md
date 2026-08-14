# Estude, Noah! — Android MVP 1

Primeira versão simples do aplicativo educacional para tablet Android.

## O que já funciona

- Tela inicial "Estude, Noah!".
- Escolha entre Português, Matemática, Ciências, História e Geografia.
- 5 questões por matéria.
- Uma questão por vez, com alternativas grandes para uso em tablet.
- Em caso de erro, permite tentar novamente.
- A pontuação registra os acertos de primeira tentativa.
- Tela final com quantidade de acertos e percentual.
- Histórico salvo localmente no tablet (até 50 atividades).
- Botão para limpar o histórico.
- Interface responsiva para tablet em orientação vertical ou horizontal.
- Funciona sem conta, servidor ou internet.

## Tecnologia

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin 9.2.1
- Gradle 9.4.1
- compileSdk 37 / targetSdk 37
- minSdk 24

## Como abrir

1. Instale uma versão atual do Android Studio compatível com AGP 9.2.
2. Extraia a pasta do projeto.
3. No Android Studio, escolha **Open** e selecione a pasta `EstudeNoahAndroid`.
4. Aguarde o Gradle Sync e a instalação do SDK 37, caso o Android Studio solicite.
5. Conecte o tablet Samsung com depuração USB ativada ou use um emulador.
6. Clique em **Run** para instalar a versão de teste.

## Como gerar um APK

No Android Studio:

**Build > Build App Bundle(s) / APK(s) > Build APK(s)**

O APK de debug costuma ser criado em:

`app/build/outputs/apk/debug/app-debug.apk`

## Próxima evolução sugerida

A próxima versão pode permitir que um adulto cadastre as perguntas diretamente no próprio aplicativo. Depois disso, podemos acrescentar importação de PDF/foto e, só então, geração por IA.
