# Análise pedagógica de vídeos do YouTube

## Escopo C1

O backend analisa vídeos **públicos** do YouTube com Gemini, considerando fala e elementos visuais. A saída é uma análise didática estruturada que poderá alimentar etapas futuras do motor pedagógico. C1 não gera atividades, não integra AVA, não automatiza sites escolares e não implementa análise de URLs genéricas.

## Fluxo

1. `POST /v1/materials/youtube/analyze` recebe somente `url`.
2. `YoutubeUrlNormalizer` exige HTTPS e host oficial exato.
3. A URL é reduzida ao formato canônico `https://www.youtube.com/watch?v=VIDEO_ID`.
4. O backend envia prompt e URI de vídeo à Gemini Interactions API.
5. Gemini processa áudio e vídeo e responde conforme JSON Schema.
6. O backend valida novamente o contrato e devolve somente o modelo público.

Não existe download, transcodificação, extração de áudio, arquivo temporário, cache ou persistência do vídeo ou da análise. A interação é enviada com `store=false`.

## Segurança da URL

Aceitos:

- `https://youtube.com/watch?v=...`;
- `https://www.youtube.com/watch?v=...`;
- `https://m.youtube.com/watch?v=...`;
- `https://youtu.be/...`.

Rejeitados antes do provedor: HTTP, outros esquemas, IPs, localhost, portas explícitas, credenciais na URL, domínios parecidos, redirecionadores, caminhos diferentes de `/watch`, IDs ausentes ou inválidos e qualquer host fora da allowlist. O serviço não segue redirecionamentos recebidos do usuário.

## Configuração e segredo

Defina `GEMINI_API_KEY` somente no ambiente seguro do backend (em produção, Secret Manager). `GEMINI_MODEL` seleciona o modelo; o padrão documentado é `gemini-3.6-flash`. O ambiente sempre prevalece.

O CI não precisa nem recebe chave Gemini: todos os testes usam cliente simulado. Um teste real futuro deverá ser opt-in, desativado por padrão, usar segredo efêmero e vídeo público não privado.

## Contrato pedagógico

`YoutubePedagogicalAnalysisPrompt` tem versão explícita. O prompt:

- trata o vídeo como fonte exclusiva;
- considera áudio e imagem;
- proíbe completar lacunas com conhecimento externo;
- pede avisos de ambiguidade;
- exige evidências com timestamps;
- identifica matéria, resumo, temas, objetivos, conceitos, relações e equívocos plausíveis;
- proíbe perguntas e atividades nesta fase.

Structured Output restringe a forma do JSON. O backend ainda faz validação semântica mínima, pois schema sintático não garante fidelidade factual.

## Falhas e resiliência

O timeout total por tentativa é de três minutos. Há no máximo uma nova tentativa para `429`, `500`, `502`, `503` e `504`; erros de entrada e autenticação não são repetidos. As categorias públicas são estáveis e não reproduzem mensagens internas do Gemini.

Vídeos privados, não listados ou inacessíveis não são suportados pela entrada direta do YouTube no Gemini. A disponibilidade desse recurso, seus limites e preços ainda podem mudar por estar em preview.

## Privacidade e retenção

O backend não armazena URL, resposta ou conteúdo em banco, disco ou cache. Logs não devem registrar chave, corpo pedagógico ou resposta integral do provedor. Em uma fase futura, autenticação, rate limiting, auditoria sem conteúdo e política de retenção devem anteceder exposição pública.

## Limitações conhecidas

- a amostragem visual do provedor pode perder mudanças rápidas de quadro;
- a qualidade depende de áudio e imagem do vídeo;
- timestamps e inferências continuam sujeitos a erro do modelo e precisam de revisão dos pais;
- vídeos privados ou não listados não funcionam;
- C1 não produz atividades nem substitui a validação pedagógica posterior.
