# Análise pedagógica de vídeos do YouTube

## Escopo C1

O backend analisa vídeos **públicos** do YouTube com Gemini, considerando fala e elementos visuais. A saída é uma análise didática estruturada que alimenta o motor pedagógico. C1 não integra AVA, não automatiza sites escolares e não implementa análise de URLs genéricas.

## Fluxo

1. `POST /v1/materials/youtube/analyze` recebe somente `url`.
2. `YoutubeUrlNormalizer` exige HTTPS e host oficial exato.
3. A URL é reduzida ao formato canônico `https://www.youtube.com/watch?v=VIDEO_ID`.
4. O backend envia prompt e URI de vídeo à Gemini Interactions API.
5. Gemini processa áudio e vídeo e responde conforme JSON Schema.
6. O backend aplica ao resultado o tipo e a URL canônica já validados no servidor, sem depender de o modelo repetir metadados de entrada.
7. O backend valida novamente o contrato e devolve somente o modelo público.
8. No fluxo Android D1B1, essa análise estruturada é enviada internamente ao endpoint de geração de atividade, sem exibir o conteúdo intermediário.

Não existe download, transcodificação, extração de áudio, arquivo temporário, cache ou persistência do vídeo ou da análise. A interação é enviada com `store=false`.

## Segurança da URL

Aceitos:

- `https://youtube.com/watch?v=...`;
- `https://www.youtube.com/watch?v=...`;
- `https://m.youtube.com/watch?v=...`;
- `https://youtu.be/...`.

Rejeitados antes do provedor: HTTP, outros esquemas, IPs, localhost, portas explícitas, credenciais na URL, domínios parecidos, redirecionadores, caminhos diferentes de `/watch`, IDs ausentes ou inválidos e qualquer host fora da allowlist. O serviço não segue redirecionamentos recebidos do usuário.

## Configuração e segredo

Defina `GEMINI_API_KEY` somente no ambiente seguro do backend (em produção, Secret Manager). `GEMINI_MODEL` seleciona o modelo e o ambiente sempre prevalece.

O CI não precisa nem recebe chave Gemini: os testes automatizados usam cliente simulado. Testes E2E reais devem ser opt-in, usar segredo protegido e vídeo público não privado.

## Contrato pedagógico

`YoutubePedagogicalAnalysisPrompt` tem versão explícita. O prompt:

- trata o vídeo como fonte exclusiva;
- considera áudio e imagem;
- proíbe completar lacunas com conhecimento externo;
- pede avisos de ambiguidade;
- exige evidências com timestamps;
- identifica matéria, resumo, temas, objetivos, conceitos, relações e equívocos plausíveis;
- proíbe perguntas e atividades nesta fase de análise.

Structured Output restringe a forma do JSON. O backend ainda faz validação semântica mínima, pois schema sintático não garante fidelidade factual.

## Falhas e resiliência

Há timeout controlado e repetição limitada apenas para falhas transitórias do provedor; erros de entrada e autenticação não são repetidos. As categorias públicas são estáveis e não reproduzem mensagens internas do Gemini.

Vídeos privados, não listados ou inacessíveis não são suportados pela entrada direta do YouTube no Gemini. A disponibilidade, os limites e preços do recurso externo podem mudar.

## Privacidade, autenticação e retenção

O backend não armazena URL, resposta ou conteúdo em banco, disco ou cache. Logs não registram chave, corpo pedagógico ou resposta integral do provedor. No modo privado atual, o endpoint é protegido por Firebase Authentication, allowlist de UID e rate limiting antes da chamada ao controller e ao Gemini. App Check permanece apenas como modo alternativo futuro do backend.

Para diagnóstico manual, `GEMINI_DIAGNOSTIC_FILE` pode apontar para um arquivo temporário. Em falhas do provedor ou de parsing, o backend grava somente metadados sanitizados: status HTTP, modelo, request id, campos superiores, estado da interação, contagem e tamanho dos blocos de texto e código interno da falha. O corpo pedagógico, headers e a API key nunca são gravados. A variável fica desabilitada por padrão e é usada pelo workflow E2E para publicar `gemini-diagnostic.json` apenas quando o arquivo existir.

## Limitações conhecidas

- a amostragem visual do provedor pode perder mudanças rápidas de quadro;
- a qualidade depende de áudio e imagem do vídeo;
- timestamps e inferências continuam sujeitos a erro do modelo e precisam de revisão dos pais;
- vídeos privados ou não listados não funcionam;
- a análise C1 não substitui a validação pedagógica posterior.
