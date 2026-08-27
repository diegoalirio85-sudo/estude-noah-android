# Resolução de links escolares para YouTube

## Objetivo

Permitir que o responsável cole no Estude, Noah! uma URL pública intermediária recebida pela escola, como uma página do Brasil Escola, e que o aplicativo encontre automaticamente o vídeo público do YouTube usado naquela página.

Fluxo:

`URL escolar pública → backend autenticado → página confiável → YouTube canônico → análise pedagógica → atividade preparada`

A reprodução continua separada: a atividade preparada salva a URL canônica do YouTube e a Home oferece **Assistir vídeo no YouTube**, abrindo o aplicativo oficial e usando a conta já conectada no tablet.

## Segurança

O endpoint `POST /v1/materials/url/resolve` é protegido pela mesma autenticação Firebase do restante do backend.

O backend não funciona como fetcher arbitrário. Ele somente abre páginas HTTPS cujo host esteja em `MATERIAL_URL_ALLOWED_HOSTS`. O padrão inicial é:

- `brasilescola.uol.com.br`;
- `*.jesuitasbrasil.org.br`.

Cada redirecionamento é validado novamente, portas personalizadas e credenciais embutidas são rejeitadas, há limite de redirecionamentos e a leitura HTML é limitada a 2 MiB. Não configure curingas amplos como `*.com`.

Links do YouTube não são buscados pelo resolvedor; eles são normalizados para `https://www.youtube.com/watch?v=<id>` e entregues ao pipeline já existente.

## O que é detectado

O resolvedor procura URLs do YouTube em `href` e `src`, incluindo:

- `youtube.com/watch?v=...`;
- `youtu.be/...`;
- `youtube.com/embed/...`;
- `youtube-nocookie.com/embed/...`.

Também pode seguir, de maneira limitada, um link externo para outro host que esteja na allowlist. Isso cobre o cenário `AVA → página educacional pública → YouTube` quando o AVA expõe o destino sem exigir sessão.

## Limite de autenticação do AVA

O Estude, Noah! não recebe nem reutiliza cookies, senha ou sessão do AVA. Se a URL escolar responder `401/403` ou ocultar o destino atrás de uma página autenticada, a resolução automática não consegue atravessar essa barreira. O usuário recebe uma mensagem específica para abrir o recurso no AVA e usar o link público de destino.

Essa limitação é intencional e evita copiar credenciais escolares para o backend.

## Android

Na Área dos Pais existe a seção **Link da escola**:

1. colar a URL HTTPS;
2. informar título opcional;
3. escolher a matéria;
4. tocar em **Preparar atividade a partir do link**.

O Android chama `/v1/materials/url/resolve`, recebe a URL canônica do YouTube e reutiliza o pipeline `/v1/materials/youtube/analyze` → `/v1/activities/generate`. A atividade salva `sourceText` como a URL final do YouTube, permitindo reprodução pelo app oficial.

## Teste de aceitação inicial

Usar a página pública:

`https://brasilescola.uol.com.br/videos/brasil-colonia-o-inicio.htm`

Resultado esperado:

- URL resolvida para `https://www.youtube.com/watch?v=ocjJ8bKEQ3Q`;
- atividade de História gerada normalmente;
- atividade preparada aparece na Home;
- **Assistir vídeo no YouTube** abre o vídeo no app oficial;
- **Fazer atividade preparada** continua funcionando.
