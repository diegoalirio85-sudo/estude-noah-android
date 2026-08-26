# Formatos de Material

## Entrada
Texto digitado/colado, voz, PDF, PPT, PPS, PPTX, DOC, DOCX, ODT, MP3, MP4 e AVI.

PPT/PPS binários legados seguem como arquivo original ao backend e usam Apache POI HSLF. PPTX permanece no fluxo OOXML separado: extração local interna e envio para `/v1/activities/from-text`. PPSX não é tratado como HSLF e não faz parte do suporte desta etapa.

## Estados internos
Selecionado, extraindo/transcrevendo, analisando, falha e pronto. Esses estados não criam telas de conteúdo intermediário.

## Controles
O responsável deve poder:
- ver arquivo/formato;
- remover ou substituir o material;

## Regra
Extração parcial ou duvidosa não deve alimentar automaticamente uma atividade como se estivesse íntegra.

Áudio/vídeo: ingestão → normalização → transcrição → análise → geração.


## Regra permanente de UX

Para texto extraído de documentos, OCR, áudio, MP3, MP4, AVI, demais formatos futuros de áudio/vídeo e YouTube, o conteúdo intermediário nunca é exibido em aba, tela, preview ou painel. Também não existe botão “ver texto extraído” nem confirmação obrigatória da transcrição.

O fluxo visível é **selecionar material → “Analisando o material e preparando a atividade…” → atividade pronta**. Extração, transcrição, OCR e análise multimodal continuam disponíveis internamente quando necessárias, sem persistência integral após o processamento e sem conteúdo integral nos logs. Diagnóstico técnico futuro deve permanecer restrito a uma função administrativa separada.
