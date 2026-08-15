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

## Backend
Materiais, atividades, resultados, IA, autenticação e sincronização.

## Área dos Pais
Interface web/Sites.

## Modelo de domínio sugerido
Material, Theme, Activity, Item e Result.

## Objetivo da refatoração
Separar responsabilidades para que extração, geração, armazenamento e sincronização sejam independentes da UI e testáveis.
