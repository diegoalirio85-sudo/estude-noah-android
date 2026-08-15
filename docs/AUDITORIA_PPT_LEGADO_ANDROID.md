# Auditoria técnica — suporte a PowerPoint legado (.ppt)

## Objetivo

Registrar a avaliação do Apache POI HSLF para leitura local de apresentações binárias PowerPoint 97–2003 no aplicativo Android Estude, Noah!, preservando o `minSdk 24` e sem alterar os demais formatos.

## Implementação avaliada

Foi implementado, em branch isolada, um leitor baseado em `HSLFSlideShow` e `HSLFTextParagraph`, percorrendo `slideShow.slides` e mantendo a ordem fornecida pelo HSLF. O caminho `.pptx` e os demais extratores não foram alterados.

Foram avaliadas estas versões oficiais:

- `org.apache.poi:poi-scratchpad:5.5.1`;
- `org.apache.poi:poi-scratchpad:5.2.5`;
- `org.apache.poi:poi-scratchpad:4.1.2`.

## Incompatibilidade comprovada

As três versões falharam durante a transformação D8 do APK com `minSdk 24`. O bytecode de `org.apache.poi.poifs.nio.CleanerUtil` usa `MethodHandle.invoke`/`invokeExact`, recurso aceito pelo Android somente a partir da API 26.

A versão 5.2.5 apresentou também a mesma incompatibilidade em uma dependência transitiva, `org.apache.logging.log4j:log4j-api:2.21.1`.

Execuções do GitHub Actions:

- run #42 — POI 5.5.1 — falha em `CleanerUtil`;
- run #43 — POI 5.2.5 — falha em `CleanerUtil` e `log4j-api`;
- run #44 — POI 4.1.2 — falha em `CleanerUtil`.

Elevar o `minSdk` para 26 eliminaria esse erro específico, mas removeria suporte a Android 7 e ampliaria o escopo da tarefa. Além disso, HSLF possui referências a `java.awt`, pacote ausente no Android, de modo que apenas compilar não provaria segurança de execução em todas as apresentações.

## Decisão

A dependência e a implementação experimental foram removidas da branch. O aplicativo permanece com o comportamento anterior e compilável. Não foi incorporada uma biblioteca abandonada nem uma adaptação parcial apresentada como suporte confiável.

## Alternativa recomendada

A alternativa tecnicamente mais segura é executar a leitura HSLF em um serviço/backend Java compatível, isolado do APK:

1. o Android envia o arquivo selecionado ao serviço autorizado;
2. o serviço valida o contêiner OLE2 e abre o PPT com Apache POI HSLF atualizado;
3. o serviço percorre os slides na ordem, extrai somente texto e devolve resultado estruturado;
4. o Android converte a resposta para o `ImportedMaterialResult` existente;
5. arquivos inválidos, protegidos, vazios ou sem texto retornam estados explícitos sem crash.

Essa solução preserva `minSdk 24`, evita dependências Java Desktop no APK e permite atualizar o parser independentemente do aplicativo. Ela exige decisões de backend, privacidade, limites de arquivo, autenticação e operação offline, portanto não deve ser introduzida silenciosamente nesta tarefa local.

Uma alternativa local só deve avançar após existir uma biblioteca Android mantida, auditável e testada para HSLF/PowerPoint binário, sem dependência de `java.awt` e compatível com API 24.
