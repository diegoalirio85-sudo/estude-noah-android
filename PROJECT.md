# Estude, Noah! — Projeto

## Visão
Sistema educacional pessoal com:
- aplicativo Android para Noah;
- futura Área dos Pais web;
- backend/API;
- motor pedagógico por IA.

## Estado atual
Já existe app Android funcional em tablet Samsung, desenvolvido em Kotlin + Jetpack Compose e compilado por GitHub Actions.

Recursos já experimentados:
- tela inicial;
- matérias;
- atividades;
- resultado/histórico;
- Área dos Pais com PIN;
- cadastro de perguntas;
- criação a partir de texto;
- voz;
- seleção de arquivos;
- extração de alguns formatos;
- apagar material extraído;
- geração local;
- tratamento diferenciado de Matemática.

## Problema identificado
O gerador local baseado em regras produziu afirmações mecânicas e próximas do texto. A próxima geração deve usar análise didática por IA antes de gerar atividades.

## Arquitetura alvo
1. Android: aluno.
2. Área dos Pais web: materiais, revisão e resultados.
3. Backend/API: autenticação, armazenamento, IA e sincronização.
4. Motor pedagógico: análise, temas, objetivos, conceitos, equívocos, geração e validação.

## Regra de atividade
- Não Matemática: 5 afirmações V/F por tema.
- Matemática: 5 exercícios novos baseados na habilidade detectada.

## Fonte oficial
O GitHub passa a ser a fonte oficial do código. Patches ZIP deixam de ser o fluxo normal.

## Fluxo de ferramentas
Work → coordenação.
Codex → desenvolvimento/testes.
GitHub → código, PRs, CI e releases.
Sites → Área dos Pais.
Android → experiência do aluno.
