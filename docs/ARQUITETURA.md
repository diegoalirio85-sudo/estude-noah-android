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

### Regra global de UX para conteúdo intermediário

Extração de texto, transcrição, reconhecimento de fala, OCR de frames, descrição visual e análise multimodal são detalhes internos do pipeline. Nenhum formato atual ou futuro deve criar aba, tela, preview, painel ou botão para exibir esse conteúdo, nem exigir sua confirmação como etapa normal. O fluxo visível é **selecionar material → analisar e preparar → atividade pronta**.

Os dados intermediários podem existir em memória pelo tempo estritamente necessário à análise pedagógica, mas não devem ser persistidos integralmente nem registrados em logs. Eventual diagnóstico técnico deverá ser uma função administrativa separada da experiência normal do aluno.

Na D1B1, a criação manual usa `data/remote`/`network`: o Android extrai texto dos formatos compatíveis, obtém um Firebase ID token imediatamente antes da chamada e envia o conteúdo ao pipeline pedagógico no Cloud Run. `.ppt` e `.pps` binários são enviados como multipart e processados exclusivamente pelo HSLF do backend. `.pptx` permanece no fluxo OOXML local; `.ppsx` não é roteado ao HSLF. Não há fallback automático para o gerador pedagógico local. App Check permanece disponível como modo futuro alternativo, não simultâneo ao Firebase Authentication no fluxo privado atual.

### Home e navegação do aluno

A Home responsiva está separada em `ui/home` e apresenta cinco áreas: agenda, materiais, central de estudos, atividades do dia e histórico recente. Dados ainda não integrados ficam em um provider demonstrativo isolado; somente o histórico recente lê dados reais por meio do contrato local existente.

As telas estruturais de revisão, conquistas e operações dos pais vivem, respectivamente, em `ui/review`, `ui/trophy` e `ui/parents`. `MainActivity` mantém a coordenação temporária de rotas e encaminha callbacks aos fluxos legados, sem mover regras pedagógicas ou persistência para a UI nova.

## Backend

O projeto JVM independente em `backend/` concentra processamento seguro de materiais, integração pedagógica com IA, autenticação e futura sincronização.

Responsabilidades atuais:
- `GET /health`;
- extração servidor-side de PowerPoint binário `.ppt`/`.pps` com Apache POI HSLF;
- análise de URLs do YouTube;
- análise documental e geração pedagógica;
- proteção dos endpoints privados por Firebase Authentication e allowlist de UID;
- processamento efêmero sem retenção permanente por padrão.

A camada `backend/activity` implementa o passo posterior à análise: modelos de requisição/resposta, prompt pedagógico versionado, integração com o cliente Gemini compartilhado e validação determinística. O fluxo permanece `Material → Analysis → Activity Generation`, sem persistência permanente do conteúdo intermediário.

A camada `backend/document` acrescenta análise pedagógica genérica de texto e a orquestração documento → C2.1. PDF, PPTX, DOCX, ODT e DOC mantêm extração local antes do envio do texto; `.ppt` e `.pps` são enviados ao backend e processados pelo HSLF existente. A conversão para o contrato pedagógico compartilhado acontece antes de delegar ao mesmo `ActivityGenerationService`, evitando um segundo motor de geração.

O upload multipart da fase inicial é uma integração de desenvolvimento. A arquitetura de produção deverá usar objetos temporários em bucket privado, exclusão após processamento e lifecycle como proteção adicional.

## Área dos Pais

A opção **Conta do backend**, protegida pelo PIN, autentica o responsável via Firebase Authentication Email/Password. A sessão é mantida pelo Firebase SDK; senha e ID token não entram nas SharedPreferences. O Cloud Run valida o ID token e autoriza somente UIDs configurados em `ALLOWED_FIREBASE_UIDS`.

A interface web/Sites permanece uma evolução futura para administração e acompanhamento.

## Agenda Vieira — Plano de Aula

A fonte da agenda diária é a rota autenticada `#/plano-aula` do TOTVS Educacional. A coleta acontece exclusivamente na WebView do usuário já autenticado: JavaScript local lê os campos visíveis do DOM e devolve `DailyLessonPlan` com aulas estruturadas. Usuário, senha, cookies, storages e headers de autenticação não são lidos nem enviados ao backend.

O plano é persistido localmente por data. Reimportar a mesma data substitui o plano anterior, sem duplicar aulas ou tarefas e sem alterar o histórico de estudos. A Home ordena as aulas pelo início, prefere `completedContent` para representar o que foi estudado e usa `plannedContent` apenas quando o realizado estiver vazio. Somente `homework` não vazio alimenta Atividades de Hoje.

Importar agenda não chama Gemini. O pipeline C2.1 só deve ser acionado posteriormente por escolha explícita de um material ou atividade.

## Modelo de domínio sugerido
Material, Theme, Activity, Item e Result.

## Objetivo da refatoração
Separar responsabilidades para que extração, geração, armazenamento e sincronização sejam independentes da UI e testáveis.
