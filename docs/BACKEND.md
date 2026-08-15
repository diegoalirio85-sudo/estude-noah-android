# Backend do Estude, Noah!

## Motivação

Processamentos incompatíveis com o Android ou que dependem de credenciais ficam fora do APK. A primeira responsabilidade real é extrair texto de PowerPoint binário `.ppt` com Apache POI HSLF. As próximas etapas poderão analisar vídeos do YouTube e executar o motor pedagógico, sempre sem expor chaves no aplicativo.

O backend é um projeto Maven/JVM independente em `backend/`. Ele não é incluído em `settings.gradle.kts` e não participa do build Android.

## Tecnologia

- Java 21;
- Spring Boot 4.0.7;
- Apache POI HSLF / `poi-scratchpad` 5.5.1;
- Maven;
- container Linux preparado para Google Cloud Run.

Execução local:

```bash
cd backend
mvn spring-boot:run
```

Container:

```bash
docker build -t estude-noah-backend backend
docker run --rm -e PORT=8080 -p 8080:8080 estude-noah-backend
```

## API

### Saúde

`GET /health`

```json
{"status":"ok","service":"estude-noah-backend"}
```

Não depende de Gemini, arquivos ou serviços externos.

### PowerPoint legado

`POST /v1/materials/ppt/extract`, com `multipart/form-data` e campo `file`.

```bash
curl -F "file=@material.ppt;type=application/vnd.ms-powerpoint" \
  http://localhost:8080/v1/materials/ppt/extract
```

A resposta contém nome seguro do arquivo, quantidade de slides, texto estruturado, slides individuais e `usableForGeneration`. O extrator percorre os slides na ordem do HSLF e agrega títulos e caixas de texto disponíveis, sem renderização e sem OCR. `usableForGeneration` exige pelo menos 140 caracteres efetivamente extraídos, em continuidade ao critério conservador do Android.

Erros:

- `400`: upload ausente, vazio ou ilegível;
- `413`: limite configurado excedido;
- `415`: extensão diferente de `.ppt`;
- `422`: PPT inválido, corrompido, protegido ou não suportado;
- `500`: falha inesperada, sem stack trace na resposta.

O multipart tem limite configurável, com padrão de 50 MB, apenas para desenvolvimento e integração inicial. Ele não é a arquitetura definitiva de produção.

## Upload de produção

Para evitar dependência permanente de limites do servidor HTTP:

1. o backend cria uma autorização curta para um objeto temporário;
2. o cliente envia diretamente para um bucket privado do Cloud Storage;
3. o backend processa o objeto;
4. o resultado textual é devolvido;
5. o objeto é excluído imediatamente;
6. uma regra de lifecycle remove resíduos como proteção adicional.

Devem existir autenticação, autorização por usuário, limite de tamanho, validação de tipo real e proteção contra abuso antes de exposição pública.

## Privacidade e segurança

O padrão é `upload → processamento → extração → exclusão`. O serviço desta fase processa o stream multipart e não implementa armazenamento permanente.

Credenciais, tokens, chaves Gemini e JSON de service account não pertencem ao repositório nem ao APK. Em Cloud Run, usar identidade do serviço e Secret Manager. O arquivo `.env.example` contém apenas nomes e valores não secretos.

Antes de produção ainda são obrigatórios autenticação, limitação de taxa, auditoria sem conteúdo escolar, política de retenção e revisão da superfície de upload.

## YouTube e Gemini

`VideoAnalysisService` define a fronteira futura para receber uma URL validada do YouTube e retornar:

```json
{
  "sourceType": "youtube",
  "sourceUrl": "https://www.youtube.com/watch?v=...",
  "title": "...",
  "subject": "...",
  "summary": "...",
  "themes": [{
    "name": "...",
    "learningObjectives": [],
    "concepts": [],
    "relationships": [],
    "likelyMisconceptions": [],
    "evidence": []
  }]
}
```

Esta fase não chama Gemini, não baixa vídeos e não extrai MP3. A implementação futura deve validar domínios/redirecionamentos, respeitar as regras da plataforma e manter evidências ligadas ao material.

## Estratégia futura para o AVA

Links intermediários do AVA Antônio Vieira devem ser resolvidos no backend com lista de destinos permitidos, limite de redirecionamentos e proteção contra SSRF. Login no AVA, captura de agenda e extensão de navegador permanecem fora desta fase.
