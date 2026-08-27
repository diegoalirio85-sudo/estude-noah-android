# Roadmap — Estude, Noah!

## Fase A — Migração e governança
- [ ] confirmar acesso ao GitHub;
- [ ] preservar baseline estável;
- [ ] adicionar documentação permanente;
- [ ] revisar CI e assinatura;
- [ ] adotar branches + PRs.

Baseline pretendido: `v3.3-stable-pre-migration` ou equivalente ao commit comprovadamente instalado e funcional.

## Fase B — Refatoração sem mudança de comportamento
Separar UI, domínio, armazenamento, materiais, geração e histórico.

## Fase C — Motor pedagógico por IA
Pipeline: extração → análise → objetivo → temas → conceitos → relações → equívocos → geração → validação → revisão.

## Fase D — Materiais multimodais
Texto, voz, PDF, DOCX, ODT, PPTX, DOC, PPT, PPS, MP3, MP4 e AVI.

### D0 — Home definitiva de cinco áreas
- [x] estrutura responsiva para tablet em paisagem e retrato;
- [x] agenda, materiais, central, atividades do dia e histórico recente;
- [x] entradas estruturais de revisão e conquistas;
- [x] Área dos Pais reposicionada para configuração e acompanhamento;
- [x] acesso privado Android → backend protegido por Firebase Auth e allowlist de UID;
- [ ] conectar providers reais de agenda, materiais e atividades;
- [ ] substituir placeholders quando os contratos remotos estiverem definidos.

### D1A — Pipeline pedagógico para documentos
- [x] análise estruturada de texto documental com fidelidade à fonte;
- [x] orquestração texto → C2.1 → atividade;
- [x] orquestração `.ppt`/`.pps` → HSLF → C2.1 → atividade;
- [x] limites explícitos sem truncamento e preparação para chunking;
- [x] D1B1: conectar a criação manual Android aos pipelines documentais e YouTube protegidos por Firebase Authentication;
- [ ] adicionar E2E manual com PDF artificial ou público.

## Fase E — Área dos Pais em Sites
Adicionar material, revisar extração, ver temas, revisar atividade, aprovar e acompanhar resultados.

## Fase F — Backend e sincronização
Área dos Pais → backend → Android → respostas → backend → painel.

## Fase G — Inteligência de aprendizagem
Identificar dificuldades, ajustar dificuldade, repetir habilidades em novos contextos e acompanhar progresso.

## Fase H — Automação escolar
Somente após o núcleo pedagógico estar estável: agenda, AVA, materiais do dia e preparação automática.
