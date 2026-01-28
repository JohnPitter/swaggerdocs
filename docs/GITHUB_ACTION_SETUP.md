# GitHub Action Setup Guide

Este guia explica como configurar o GitHub Action para publicar automaticamente seu Swagger/OpenAPI spec no SwaggerDocs Portal.

## Quick Start (5 minutos)

### 1. Copie o workflow

Copie o arquivo `github-action-template.yml` para seu repositório:

```bash
mkdir -p .github/workflows
cp github-action-template.yml .github/workflows/swagger-publish.yml
```

### 2. Configure a variável

No seu repositório GitHub:
1. Vá em **Settings** > **Secrets and variables** > **Actions**
2. Clique na aba **Variables**
3. Clique em **New repository variable**
4. Nome: `SWAGGERDOCS_URL`
5. Valor: URL do portal (ex: `https://swaggerdocs.empresa.com`)

### 3. Ajuste o path do arquivo

Edite o workflow e ajuste a seção `paths` para corresponder à localização do seu arquivo swagger:

```yaml
on:
  push:
    branches: [main]
    paths:
      - 'seu/path/swagger.json'  # Ajuste aqui
```

### 4. Commit e push

```bash
git add .github/workflows/swagger-publish.yml
git commit -m "ci: add swagger publish workflow"
git push
```

Pronto! A cada push que modificar o arquivo swagger, ele será publicado automaticamente.

---

## Localizações Suportadas

O template procura automaticamente nestas localizações:
- `docs/swagger.json`
- `docs/swagger.yaml`
- `docs/openapi.json`
- `docs/openapi.yaml`
- `src/main/resources/openapi.yaml`
- `api/swagger.json`

## Formatos Suportados

- **JSON**: `.json` - Usado diretamente
- **YAML**: `.yaml` / `.yml` - Convertido automaticamente para JSON

## Estrutura do Payload

O Action envia este payload para o portal:

```json
{
  "appName": "nome-do-repositorio",
  "team": "owner-do-repositorio",
  "environment": "production",
  "version": "extraído do swagger.info.version",
  "swagger": { ... conteúdo do arquivo ... },
  "metadata": {
    "commitHash": "abc123...",
    "branch": "main",
    "pipelineUrl": "https://github.com/.../actions/runs/123"
  }
}
```

## Resultado

Após a publicação, o Action exibe:
- **Status**: ACCEPTED, ACCEPTED_WITH_WARNINGS, ou REJECTED
- **Quality Score**: Pontuação de qualidade (0-100)
- **Link direto** para a documentação no portal

## Troubleshooting

### "SWAGGERDOCS_URL variable not set"

Configure a variável conforme o passo 2 acima.

### "No swagger file found"

Verifique se o arquivo swagger existe em uma das localizações suportadas, ou ajuste o workflow.

### HTTP 400 - Bad Request

O arquivo swagger pode estar mal formatado. Valide em https://editor.swagger.io

### HTTP 503 - Service Unavailable

O portal pode estar com problemas de conexão com o GitHub. Tente novamente.

## Versão Simplificada

Para quem prefere algo mais direto, use `github-action-simple.yml`:

```yaml
name: Publish Swagger

on:
  push:
    branches: [main]
    paths: ['docs/swagger.json']

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Publish
        run: |
          curl -X POST "${{ vars.SWAGGERDOCS_URL }}/api/swaggers" \
            -H "Content-Type: application/json" \
            -d "{
              \"appName\": \"${{ github.event.repository.name }}\",
              \"team\": \"${{ github.repository_owner }}\",
              \"swagger\": $(cat docs/swagger.json)
            }"
```

## Suporte

Em caso de dúvidas, abra uma issue no repositório do SwaggerDocs Portal.
