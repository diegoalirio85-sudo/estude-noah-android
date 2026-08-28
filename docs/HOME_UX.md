# Home de cinco áreas

## Propósito

A Home é o ponto de partida diário do aluno em tablets. Ela organiza informação e ações em cinco áreas, sem depender de integrações externas nesta fase:

1. **Agenda de hoje** — espaço preparado para compromissos escolares;
2. **Materiais de hoje** — prévia de materiais, inclusive YouTube;
3. **Central de estudos** — criação, prática, revisão, conquistas e acesso protegido dos pais;
4. **Atividades de hoje** — sugestões de continuidade;
5. **Últimas 10 atividades** — dados reais do histórico local existente.

Materiais de hoje ainda usa conteúdo demonstrativo isolado em `HomePreviewData`. Agenda e atividades do dia passam a consumir o `DailyLessonPlan` persistido quando o responsável importa uma data no Agenda Vieira.

Quando existir um Plano de Aula Vieira importado para a data atual, **Agenda de Hoje** mostra horário, disciplina e conteúdo realizado, em ordem cronológica; se o realizado estiver vazio, usa o previsto. **Atividades de Hoje** mostra exclusivamente aulas com Lição de Casa não vazia. A ausência de lição nunca produz tarefa artificial.

A importação é estrutural e local. Ela não gera questões automaticamente nem envia a sessão TOTVS ao backend.

Cada lição de casa permanece visível depois de concluída e pode ser marcada manualmente como feita ou desfeita. O quadro apresenta o resumo `x de y concluídas`, sem pontuação, competição ou penalidade. A conclusão é persistida localmente por uma chave estável derivada da data, aula e texto integral da tarefa; uma alteração substancial do texto produz uma chave nova e evita concluir automaticamente uma tarefa diferente.

## Responsividade

Em largura de tablet/paisagem, os painéis superior e inferior aparecem em pares. Em retrato ou telas estreitas, esses painéis são empilhados. A Central de estudos permanece em destaque nos dois formatos.

## Navegação preservada

- **Criar nova atividade** abre a importação/entrada de material já existente;
- **Praticar agora** mantém a seleção de matéria e o questionário local;
- a atividade preparada continua disponível e consumida pelo mesmo fluxo;
- **Últimas 10 atividades** usa `LocalPreferencesRepository.loadHistory()` sem mudar chaves ou formatos;
- **Área dos Pais** continua protegida pelo PIN existente;
- importação manual, perguntas locais e alteração de PIN continuam acessíveis.

## Telas estruturais

**Revisar** oferece os pontos de entrada para matéria e período e reutiliza a prática existente enquanto os filtros ainda não possuem domínio próprio.

**Conquistas** resume constância e evolução exclusivamente a partir do histórico local. Não há ranking, comparação entre alunos ou mecânica competitiva.

**Área dos Pais** passa a comunicar configuração e acompanhamento. Recursos locais anteriores permanecem como ferramentas administrativas para evitar perda funcional.

## Fora do escopo D0

Não há sincronização com agenda, AVA, backend, Gemini ou YouTube; não há nova persistência; não há mudança no motor pedagógico. Esses contratos serão conectados em fases posteriores.
