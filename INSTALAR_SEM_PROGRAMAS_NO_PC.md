# Instalar no tablet sem instalar programas no computador

Este projeto pode ser compilado pelo GitHub Actions. O computador precisa apenas de um navegador.

## 1. Criar um repositório no GitHub

1. Entre em https://github.com no navegador.
2. Crie um repositório novo, por exemplo `estude-noah-android`.
3. Envie para o repositório **o conteúdo da pasta `EstudeNoahAndroid`**.
   - É importante que `app`, `.github`, `build.gradle.kts` e os demais arquivos fiquem na raiz do repositório.

## 2. Gerar o APK na nuvem

Ao enviar os arquivos para a branch `main`, o fluxo **Gerar APK - Estude Noah** será executado automaticamente.

Também é possível iniciar manualmente:

1. Abra a aba **Actions** do repositório.
2. Escolha **Gerar APK - Estude Noah**.
3. Clique em **Run workflow**.

Quando a execução ficar verde:

1. Abra a execução concluída.
2. Na área **Artifacts**, baixe **Estude-Noah-APK**.
3. O GitHub baixa um ZIP contendo `app-debug.apk`.

## 3. Instalar no tablet Samsung

1. Abra o GitHub no navegador do tablet e baixe o artefato.
2. Extraia o ZIP pelo app **Meus Arquivos**.
3. Toque em `app-debug.apk`.
4. Se o Android bloquear a instalação, autorize temporariamente a instalação de apps desconhecidos para o navegador ou para o Meus Arquivos.
5. Instale **Estude, Noah!**.

Depois da instalação, você pode revogar novamente a permissão para instalar apps desconhecidos.

## Observação

Este APK é uma versão de teste (`debug`) e é adequado para instalar e testar diretamente no seu tablet. Em uma etapa posterior podemos gerar uma versão de distribuição assinada com uma chave própria.
