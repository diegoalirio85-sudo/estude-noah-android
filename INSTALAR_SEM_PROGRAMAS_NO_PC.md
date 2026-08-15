# Instalar no tablet sem instalar programas no computador

O APK pode ser compilado pelo GitHub Actions e instalado no tablet usando apenas um navegador.

Repositório oficial: [diegoalirio85-sudo/estude-noah-android](https://github.com/diegoalirio85-sudo/estude-noah-android).

## 1. Gerar o APK na nuvem

Um push para `main` inicia automaticamente o workflow **Gerar APK - Estude Noah**.

Para validar uma branch antes do merge:

1. abra a aba **Actions** do repositório;
2. escolha **Gerar APK - Estude Noah**;
3. clique em **Run workflow**;
4. selecione explicitamente a branch desejada;
5. confirme em **Run workflow**;
6. aguarde todas as etapas ficarem verdes.

## 2. Baixar o APK

1. abra a execução concluída;
2. confirme que a execução corresponde à branch e ao commit desejados;
3. na área **Artifacts**, baixe **Estude-Noah-APK**;
4. extraia o ZIP para obter `app-debug.apk`.

Os artefatos ficam disponíveis pelo período configurado no workflow, atualmente 30 dias.

## 3. Instalar no tablet Samsung

1. transfira ou baixe o ZIP no tablet;
2. extraia o arquivo pelo aplicativo **Meus Arquivos**;
3. toque em `app-debug.apk`;
4. se o Android bloquear a instalação, autorize temporariamente a instalação de apps desconhecidos para o navegador ou **Meus Arquivos**;
5. confirme a instalação;
6. revogue novamente essa permissão depois de concluir.

## Atualizar uma instalação existente

A atualização só funciona diretamente quando o novo APK mantém:

- o package `com.estudenoah.app`;
- uma assinatura compatível com a instalação existente;
- um `versionCode` adequado ao fluxo de atualização.

Antes de atualizar, preserve os dados importantes e confirme que o APK veio da execução correta. Depois, verifique no aplicativo:

- histórico;
- PIN da Área dos Pais;
- perguntas cadastradas;
- atividade preparada;
- abertura e execução de uma atividade.

Não desinstale a versão atual como primeira tentativa: a desinstalação pode remover dados locais.

## Observação de segurança

O artefato atual é um APK de debug, destinado ao uso e teste controlado deste projeto. A migração para uma assinatura de release exige planejamento separado para não interromper atualizações existentes.

Nunca compartilhe senhas, tokens ou chaves pelo repositório, APK ou logs do GitHub Actions.
