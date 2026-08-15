# AGENTS.md — Estude, Noah!

## Missão
O **Estude, Noah!** é um sistema educacional para transformar materiais realmente estudados em atividades curtas, relevantes e adequadas ao aluno.

## Regra de ouro
**Nunca gerar atividades mecanicamente a partir de trechos do texto.**

Antes de gerar qualquer atividade:
1. compreender o material;
2. identificar matéria, assunto, temas e subtemas;
3. inferir o objetivo de aprendizagem;
4. identificar conceitos essenciais e relações;
5. identificar equívocos plausíveis;
6. somente então gerar a atividade.

## Regras pedagógicas
### Português, Ciências, História e Geografia
- 5 afirmações de Verdadeiro/Falso por tema;
- não copiar frases do material;
- exigir compreensão, relação, causa/consequência, comparação, função, aplicação ou inferência;
- afirmações falsas devem representar erros conceituais plausíveis;
- evitar falsidade por simples negação ou troca trivial de número/nome;
- cada item deve ter resposta, justificativa, evidência no material, tema e dificuldade.

### Matemática
- não usar Verdadeiro/Falso como padrão;
- identificar primeiro a habilidade matemática;
- criar 5 exercícios novos que avaliem a mesma habilidade;
- preferir valores e situações diferentes dos exemplos;
- cada item deve ter resposta e solução.

## Fidelidade ao material
O material fornecido é a fonte primária. Não inventar conteúdo não sustentado. Se a extração estiver duvidosa, não gerar atividade com aparência de certeza.

## Fluxo obrigatório
Material → extração/transcrição → análise didática → temas → objetivos → conceitos → relações → possíveis equívocos → geração → validação → revisão dos pais → aluno.

## Materiais
Texto, voz, PDF, PPT, PPTX, DOC, DOCX, ODT, MP3, MP4 e AVI.

Deve existir a ação **Apagar material extraído**, apagando a extração sem apagar automaticamente matéria e título.

## Android
- cliente principal do aluno;
- pacote `com.estudenoah.app` não deve ser alterado sem autorização;
- preservar atualização do APK;
- experiência simples para tablet.

## Engenharia
- há dois `build.gradle.kts`: raiz e `app/`; nunca substituir um pelo outro;
- `main` deve permanecer compilável;
- preferir branch + Pull Request para mudanças relevantes;
- toda alteração Android deve passar pelo GitHub Actions;
- não colocar segredos/chaves de IA no APK;
- não continuar concentrando responsabilidades em `MainActivity.kt`.

## Antes de alterar código
Verificar impacto em: assinatura, dados locais, migrações, CI, compatibilidade de atualização e comportamento pedagógico.

## Depois de alterar código
Compilar, testar, revisar diff, registrar mudanças/riscos e explicar como testar no tablet.
