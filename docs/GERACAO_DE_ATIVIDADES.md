# Geração de Atividades

## Não Matemática
Por tema:
- 5 afirmações V/F;
- resposta;
- explicação;
- evidência;
- dificuldade;
- habilidade avaliada.

Rejeitar itens duplicados, literais, ambíguos ou sem fundamento.

## Matemática
Por item:
- problema;
- resposta;
- solução;
- habilidade;
- dificuldade;
- conceito-fonte.

## Revisão dos pais
Permitir editar, excluir, regenerar, mudar dificuldade e aprovar.

## Implementação backend C2

O endpoint `POST /v1/activities/generate` recebe `grade`, `subject`, `source` e análise pedagógica estruturada. A geração usa Structured Output e passa por validação de quantidade, campos obrigatórios, dificuldade, equilíbrio V/F, regras de Matemática e duplicação textual. Saídas incompletas não são entregues como válidas. Consulte `ACTIVITY_GENERATION.md`.
