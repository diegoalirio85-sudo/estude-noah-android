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

Na D1B1, a criação manual usa `data/remote`/`network`: o Android extrai texto dos formatos compatíveis, obtém App Check imediatamente antes da chamada e envia o conteúdo ao pipeline pedagógico no Cloud Run. `.ppt` binário é enviado como multipart e processado exclusivamente pelo HSLF do backend. Não há fallback automático para o gerador pedagógico local.

### Home e navegação do aluno

A Home responsiva está separada em `ui/home` e apresenta cinco áreas: agenda, materiais, central de estudos, atividades do dia e histórico recente. Dados ainda não integrados ficam em um provider demonstrativo isolado; somente o histórico recente lê dados reais por meio do contrato local existente.

As telas estruturais de revisão, conquistas e operações dos pais vivem, respectivamente, em `ui/review`, `ui/trophy` e `ui/parents`. `MainActivity` mantém a coordenação temporária de rotas e encaminha callbacks aos fluxos legados, sem mover regras pedagógicas ou persistência para a UI nova.

## Backend

O projeto JVM independente em `backend/` concentra processamento seguro de materiais, futura integração com IA, autenticação e sincronização.

Responsabilidades iniciais:
- `GET /health`;
- extração servidor-side de PowerPoint binário `.ppt` com Apache POI HSLF;
- contrato futuro para análise de URLs do YouTube;
- processamento efêmero sem retenção permanente por padrão.

A camada `backend/activity` implementa o passo posterior à análise: modelos de requisição/resposta, prompt pedagógico versionado, integração com o cliente Gemini compartilhado e validação determinística. O fluxo permanece `Material → Analysis → Activity Generation`, sem persistência e sem conexão Android nesta fase.

A camada `backend/document` acrescenta análise pedagógica genérica de texto e a orquestração documento → C2.1. PDF, PPTX, DOCX, ODT e DOC mantêm extração local antes do envio do texto; `.ppt` é enviado ao backend e processado pelo HSLF existente. A conversão para o contrato pedagógico compartilhado acontece antes de delegar ao mesmo `ActivityGenerationService`, evitando um segundo motor de geração.

O upload multipart da fase inicial é uma integração de desenvolvimento. A arquitetura de produção deverá usar objetos temporários em bucket privado, exclusão após processamento e lifecycle como proteção adicional.

## Área dos Pais
Interface web/Sites.

## Modelo de domínio sugerido
Material, Theme, Activity, Item e Result.

## Objetivo da refatoração
Separar responsabilidades para que extração, geração, armazenamento e sincronização sejam independentes da UI e testáveis.

