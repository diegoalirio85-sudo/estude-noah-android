# E2E manual — PDF no Android

## Objetivo

Validar no tablet real o caminho completo:

`PDF físico → PdfRenderer/PdfRendererPreV no Android → texto extraído localmente → /v1/activities/from-text → atividade pedagógica → atividade preparada → execução`.

Este teste não pode ser substituído por copiar e colar o texto do PDF. O objetivo é provar especificamente a extração do arquivo PDF pelo Android.

## Fixture oficial

Arquivo:

`test-fixtures/e2e/e2e-material-sistema-solar.pdf`

Características:

- PDF físico válido, com 2 páginas;
- conteúdo sintético e sem dados pessoais;
- assunto: Sistema Solar;
- SHA-256: `58eabb4224f52a1d4a7e20a81d4f82f463fa198ed551cc1c143f8818b66c54f3`;
- frase de controle: `NOAH-PDF-2026-SISTEMA-SOLAR`.

O conteúdo inclui, entre outros fatos verificáveis:

- o Sol é uma estrela;
- a Terra é o terceiro planeta a partir do Sol;
- Mercúrio é o planeta mais próximo do Sol;
- Netuno é o mais distante entre os oito planetas;
- rotação ocorre em torno do próprio eixo;
- translação ocorre ao redor do Sol;
- a translação da Terra dura aproximadamente 365 dias.

## Pré-requisitos

1. APK da branch/PR em teste instalado no tablet.
2. Conta do backend autenticada no Estude, Noah!.
3. Internet disponível.
4. A fixture PDF salva localmente no tablet.

## Procedimento

1. Abra **Área dos Pais**.
2. Entre em **Criar atividade a partir de material**.
3. Selecione **Ciências**.
4. Toque em **Adicionar PDF, Office, ODT...** e escolha `e2e-material-sistema-solar.pdf`.
5. Aguarde a análise automática do arquivo.
6. Confirme que a tela de revisão recebe uma atividade gerada a partir do PDF.
7. Verifique que as afirmações/questões se relacionam concretamente ao conteúdo da fixture, por exemplo Sol, Terra, Mercúrio, Netuno, rotação ou translação.
8. Salve como **atividade preparada**.
9. Volte à Home e execute **Fazer atividade preparada** até o final.
10. Confirme resultado e registro no histórico.

## Critérios de aprovação

O E2E é aprovado somente se todos estes itens ocorrerem:

- o PDF é selecionado como arquivo, sem copiar seu texto manualmente;
- a extração local produz texto suficiente para iniciar a geração;
- o backend devolve uma atividade válida;
- as questões demonstram fidelidade ao conteúdo específico da fixture;
- a atividade pode ser salva, executada e concluída;
- não há fallback silencioso para o gerador local;
- o conteúdo integral extraído não fica exposto como preview nem persistido como `sourceText` do arquivo.

## Falha do mecanismo PDF

Se o app exibir a mensagem de que o tablet precisa do mecanismo PDF mais recente do Android, o E2E **não está aprovado**. Nesse caso deve-se investigar a compatibilidade do dispositivo/Android; não se deve contornar o teste colando o texto da fixture.

## Escopo de mídia

MP3, MP4 e AVI permanecem fora da prioridade atual. Este E2E cobre somente o pipeline documental de PDF.
