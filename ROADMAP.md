# Roadmap — Estude, Noah!

## Fase A — Migração e governança
- [x] confirmar acesso ao GitHub;
- [x] preservar baseline estável;
- [x] adicionar documentação permanente;
- [x] revisar CI e assinatura;
- [x] adotar branches + PRs.

Baseline estável atual: `main`, após os pipelines privados do PR #19 e a reprodução de YouTube pelo app oficial do PR #20.

## Fase B — Refatoração sem mudança de comportamento
Separar UI, domínio, armazenamento, materiais, geração e histórico.

## Fase C — Motor pedagógico por IA
Pipeline: extração → análise → objetivo → temas → conceitos → relações → equívocos → geração → validação → revisão.

## Fase D — Materiais multimodais
Escopo ativo: texto, voz, PDF, DOCX, ODT, PPTX, DOC, PPT, PPS e links públicos do YouTube.

MP3, MP4 e AVI ficam **adiados sem prioridade atual**. Não condicionam a evolução das próximas fases.

### D0 — Home definitiva de cinco áreas
- [x] estrutura responsiva para tablet em paisagem e retrato;
- [x] agenda, materiais, central, atividades do dia e histórico recente;
- [x] entradas estruturais de revisão e conquistas;
- [x] Área dos Pais reposicionada para configuração e acompanhamento;
- [x] acesso privado Android → backend protegido por Firebase Auth e allowlist de UID;
- [x] atividades preparadas a partir de YouTube podem abrir o vídeo no app oficial, usando a sessão Premium já conectada sem compartilhar credenciais;
- [ ] conectar providers reais de agenda, materiais e atividades;
- [ ] substituir placeholders quando os contratos remotos estiverem definidos.

### D1A — Pipeline pedagógico para documentos
- [x] análise estruturada de texto documental com fidelidade à fonte;
- [x] orquestração texto → C2.1 → atividade;
- [x] orquestração `.ppt`/`.pps` → HSLF → C2.1 → atividade;
- [x] limites explícitos sem truncamento e preparação para chunking;
- [x] D1B1: conectar a criação manual Android aos pipelines documentais e YouTube protegidos por Firebase Authentication;
- [x] fixture PDF sintética e roteiro estrito de E2E adicionados ao repositório;
- [ ] executar e aprovar no tablet o E2E `PDF → extração Android → backend → atividade`, conforme `docs/E2E_PDF_ANDROID.md`.

### Próxima prioridade após o E2E de PDF

Conectar a Home aos **materiais reais da escola**, começando pelo contrato/provider de materiais. Em seguida, avançar para sincronização backend ↔ Android e acompanhamento de resultados.

## Fase E — Área dos Pais em Sites
Adicionar material, revisar extração, ver temas, revisar atividade, aprovar e acompanhar resultados.

## Fase F — Backend e sincronização
Área dos Pais → backend → Android → respostas → backend → painel.

## Fase G — Inteligência de aprendizagem
Identificar dificuldades, ajustar dificuldade, repetir habilidades em novos contextos e acompanhar progresso.

## Fase H — Automação escolar
Somente após o núcleo pedagógico estar estável: agenda, AVA, materiais do dia e preparação automática.
