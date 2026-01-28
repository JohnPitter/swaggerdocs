# Design: Sincronização com GitHub Remoto

**Data:** 2026-01-28
**Status:** Aprovado

## Contexto

A aplicação SwaggerDocs será deployada em Kubernetes sem acesso a configurações de PersistentVolume. O storage local do container é efêmero, perdendo dados ao reiniciar. A solução é sincronizar o repositório Git local com um repositório remoto no GitHub.

## Decisões

| Aspecto | Decisão |
|---------|---------|
| Instâncias | Uma única (sem conflitos de merge) |
| Sincronização | Síncrona (push imediato a cada save) |
| Retry | 3 tentativas com backoff exponencial (1s, 2s, 4s) |
| Autenticação | `GITHUB_TOKEN` via variável de ambiente |
| Storage | Repositório central único (ex: `org/api-specs`) |
| Branch | `main` (configurável) |
| Leitura | Disco local (clone no startup) |

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           GitHub (Times)                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │ repo-app-a   │    │ repo-app-b   │    │ repo-app-c   │              │
│  │ (Time Alpha) │    │ (Time Beta)  │    │ (Time Gamma) │              │
│  │              │    │              │    │              │              │
│  │ CI Pipeline  │    │ CI Pipeline  │    │ CI Pipeline  │              │
│  │ + GH Action  │    │ + GH Action  │    │ + GH Action  │              │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘              │
│         │                   │                   │                       │
└─────────┼───────────────────┼───────────────────┼───────────────────────┘
          │ POST /api/swaggers│                   │
          └───────────────────┼───────────────────┘
                              ▼
                 ┌────────────────────────┐
                 │   SwaggerDocs Portal   │
                 │   (K8s Container)      │
                 │                        │
                 │  • Valida qualidade    │
                 │  • Detecta breaking    │
                 │  • Push para central   │
                 └───────────┬────────────┘
                             │ git push (sync)
                             ▼
                 ┌────────────────────────┐
                 │  org/api-specs (GitHub)│
                 │                        │
                 │  app-a/                │
                 │    └─ swagger.json     │
                 │    └─ metadata.json    │
                 │  app-b/                │
                 │    └─ swagger.json     │
                 │  app-c/                │
                 └────────────────────────┘
```

## Fluxos

### Fluxo 1: Submissão (CI/CD dos times)

```
GitHub Action ──► POST /api/swaggers ──► Valida ──► Push repo central
```

### Fluxo 2: Consulta (Portal Web)

```
Browser ──► GET /           ──► Lista de APIs (portal)
        ──► GET /docs/{app} ──► Swagger UI / Redoc
```

## Componentes

### Arquivos a Criar/Modificar

```
src/main/java/com/swaggerdocs/
├── config/
│   └── GitRemoteConfig.java        # [NOVO] Configurações do remote
├── service/
│   └── GitStorageService.java      # [MODIFICAR] Adicionar sync remoto
└── exception/
    └── GitSyncException.java       # [NOVO] Exceção específica
```

### Configuração

```yaml
# application.yml
swaggerdocs:
  storage:
    path: ${user.home}/.swaggerdocs/storage
  git:
    remote:
      enabled: true
      url: ${GIT_REMOTE_URL:}
      branch: ${GIT_REMOTE_BRANCH:main}
      token: ${GITHUB_TOKEN:}
    retry:
      max-attempts: 3
      delay-ms: 1000
```

### Mudanças no GitStorageService

| Método | Mudança |
|--------|---------|
| `init()` | Clone se não existe, pull se existe |
| `save()` | Após commit local → push com retry |
| `cloneOrPull()` | **NOVO** - Sincroniza com remote no startup |
| `pushWithRetry()` | **NOVO** - Push com backoff exponencial |

## GitHub Action Template

Para os times adicionarem em seus repositórios:

```yaml
# .github/workflows/swagger-publish.yml
name: Publish Swagger to Portal

on:
  push:
    branches: [main]
    paths:
      - 'docs/swagger.json'
      - 'openapi.yaml'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Publish to SwaggerDocs
        run: |
          curl -X POST "${{ vars.SWAGGERDOCS_URL }}/api/swaggers" \
            -H "Content-Type: application/json" \
            -d @- << EOF
          {
            "appName": "${{ github.event.repository.name }}",
            "team": "${{ github.repository_owner }}",
            "environment": "production",
            "version": "${{ github.sha }}",
            "commitHash": "${{ github.sha }}",
            "branch": "${{ github.ref_name }}",
            "pipelineUrl": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}",
            "swagger": $(cat docs/swagger.json)
          }
          EOF
```

## Segurança

### Token do GitHub (Fine-grained PAT)

| Permissão | Escopo |
|-----------|--------|
| Contents | Read & Write |
| Metadata | Read |

Escopo limitado apenas ao repositório `org/api-specs`.

### Tratamento de Erros

| Cenário | Comportamento |
|---------|---------------|
| GitHub fora do ar | Retry 3x → 503 Service Unavailable |
| Token inválido | 500 + log de erro |
| Repo não existe | Falha no startup (fail fast) |
| Branch não existe | Cria automaticamente |
| Swagger inválido | 400 Bad Request |

## Configuração Kubernetes

```yaml
# Exemplo de Secret
apiVersion: v1
kind: Secret
metadata:
  name: swaggerdocs-secrets
type: Opaque
stringData:
  GITHUB_TOKEN: ghp_xxxxxxxxxxxx
  GIT_REMOTE_URL: https://github.com/org/api-specs.git
```

```yaml
# Exemplo de Deployment (parcial)
spec:
  containers:
    - name: swaggerdocs
      envFrom:
        - secretRef:
            name: swaggerdocs-secrets
```
