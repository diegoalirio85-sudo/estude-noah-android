# Arquitetura Alvo

## Android
Camadas pretendidas:
- `ui/student`
- `ui/activity`
- `ui/history`
- `ui/parent`
- `domain`
- `data/local`
- `data/remote`
- `material`
- `pedagogy`

O Android permanece o cliente do aluno. Segredos e processamento dependente de Java Desktop não pertencem ao APK.

## Backend

O projeto JVM independente em `backend/` concentra processamento seguro de materiais, futura integração com IA, autenticação e sincronização.

Responsabilidades iniciais:
- `GET /health`;
- extração servidor-side de PowerPoint binário `.ppt` com Apache POI HSLF;
- contrato futuro para análise de URLs do YouTube;
- processamento efêmero sem retenção permanente por padrão.

A camada `backend/activity` implementa o passo posterior à análise: modelos de requisição/resposta, prompt pedagógico versionado, integração com o cliente Gemini compartilhado e validação determinística. O fluxo permanece `Material → Analysis → Activity Generation`, sem persistência e sem conexão Android nesta fase.

O upload multipart da fase inicial é uma integração de desenvolvimento. A arquitetura de produção deverá usar objetos temporários em bucket privado, exclusão após processamento e lifecycle como proteção adicional.

## Área dos Pais
Interface web/Sites.

## Modelo de domínio sugerido
Material, Theme, Activity, Item e Result.

## Objetivo da refatoração
Separar responsabilidades para que extração, geração, armazenamento e sincronização sejam independentes da UI e testáveis.
