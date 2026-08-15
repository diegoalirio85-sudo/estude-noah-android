# Processo de Releases

## Objetivo

Entregar APKs reproduzíveis e testáveis sem interromper a atualização do aplicativo já instalado no tablet nem perder dados locais.

## Estado atual

- package / applicationId: `com.estudenoah.app`;
- versão: `3.3` (`versionCode 6`);
- artefato atual: APK de debug;
- chave usada pelo build: `app/debug.keystore`;
- workflow efetivo: `.github/workflows/build-apk.yml`;
- artefato publicado: `Estude-Noah-APK`;
- retenção configurada no GitHub Actions: 30 dias.

A chave de debug preserva a atualização sobre instalações assinadas com a mesma chave, mas não deve ser tratada como assinatura de produção.

## Fluxo obrigatório

1. criar uma branch a partir de `main`;
2. implementar mudanças incrementais;
3. revisar o diff e confirmar o escopo;
4. executar os testes aplicáveis;
5. abrir Pull Request para `main`;
6. executar **Gerar APK - Estude Noah** na branch quando o gatilho automático não iniciar;
7. exigir build verde e publicação do APK;
8. revisar e mesclar o Pull Request;
9. confirmar o build automático pós-merge em `main`;
10. testar o APK no tablet quando houver mudança funcional, de dados, versão, assinatura ou atualização.

## Build no GitHub Actions

O workflow:

1. baixa o código;
2. configura Java 17;
3. verifica o Android SDK disponível;
4. configura Gradle 9.4.1;
5. executa `gradle --no-daemon :app:assembleDebug --stacktrace`;
6. publica `app/build/outputs/apk/debug/app-debug.apk`.

Execuções manuais devem selecionar explicitamente a branch que será validada. Um build verde de outra branch ou de um commit anterior não valida o estado atual.

## Checklist antes do merge

- diff contém somente o escopo aprovado;
- package `com.estudenoah.app` preservado, salvo autorização explícita;
- `versionCode` e `versionName` alterados somente quando planejado;
- `app/debug.keystore` e configuração de assinatura preservadas;
- chaves e formatos de `SharedPreferences` preservados ou migrados explicitamente;
- código, recursos e dois arquivos `build.gradle.kts` revisados nos locais corretos;
- nenhum segredo incluído no APK;
- build da branch concluído com sucesso;
- artefato `Estude-Noah-APK` publicado;
- riscos e instruções de teste registrados no Pull Request.

## Checklist depois do merge

- build de `main` concluído com sucesso;
- APK pós-merge publicado;
- commit de merge e execução do workflow registrados;
- teste de instalação/atualização realizado no tablet quando aplicável;
- histórico, PIN, perguntas e atividade preparada preservados;
- resultado do teste registrado antes de criar uma tag estável.

## Baseline e tags

O snapshot `3e88bdaed5c61622d43d5050ff37965a66d0a254` é apenas o candidato documental ao baseline pré-migração.

A tag sugerida `v3.3-stable-pre-migration` só deve ser criada depois de confirmar que o commit escolhido corresponde ao APK efetivamente instalado e validado no tablet. Não mover nem recriar uma tag estável para apontar a outro commit.

## Versionamento

- aumentar `versionCode` para cada APK destinado a atualizar uma instalação anterior;
- usar `versionName` para a versão legível;
- não alterar versão em PR exclusivamente documental ou estrutural sem impacto no aplicativo;
- documentar mudanças de versão e compatibilidade de atualização no Pull Request.

## Assinatura

Trocar a chave de assinatura pode impedir a instalação sobre o aplicativo existente. Qualquer migração para uma chave de release exige um plano separado, backup, teste de atualização e estratégia de recuperação.

Nunca publicar senhas, tokens ou chaves de IA no aplicativo ou nos logs de CI.

## Recuperação

Se um build pós-merge falhar, não produzir uma release a partir dele. Investigar em branch própria e corrigir por novo Pull Request. Reverter somente com escopo explícito e sem apagar histórico Git.
