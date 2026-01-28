# SwaggerDocs Portal

A centralized API documentation portal for enterprises. Applications send their OpenAPI/Swagger specs via CI pipelines, and this portal validates, versions, and displays them.

[![CI](https://github.com/JohnPitter/swaggerdocs/actions/workflows/ci.yml/badge.svg)](https://github.com/JohnPitter/swaggerdocs/actions/workflows/ci.yml)

## Features

- **Multi-Version Support**: Swagger 2.0, OpenAPI 3.0, and OpenAPI 3.1
- **Centralized Documentation**: Single portal for all your microservices APIs
- **Version History**: Git-based versioning with full history tracking
- **Breaking Change Detection**: Automatic detection of breaking changes between versions
- **Quality Scoring**: Validates API specs for descriptions, examples, error responses, schemas
- **Multiple Viewers**: Swagger UI and Redoc support
- **Request Validation**: Proper 400 responses with detailed error messages
- **CI/CD Integration**: GitHub Actions templates for automated publishing
- **Remote Sync**: Optional sync to GitHub repository for persistence in Kubernetes

## Tech Stack

- Java 17+
- Spring Boot 3.2.x
- JGit for Git-based storage
- Jakarta Validation for request validation
- Thymeleaf + WebJars (Swagger UI, Redoc)
- Lombok

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+

### Run Locally

```bash
# Clone the repository
git clone https://github.com/JohnPitter/swaggerdocs.git
cd swaggerdocs

# Run the application
mvn spring-boot:run

# Access the portal
open http://localhost:8080
```

### Run with Docker

```bash
docker build -t swaggerdocs .
docker run -p 8080:8080 swaggerdocs
```

## Configuration

### Application Properties

```yaml
# application.yml
swaggerdocs:
  storage:
    path: ~/.swaggerdocs/storage  # Local storage path

  git:
    remote:
      enabled: false              # Enable GitHub sync
      url: ""                     # GitHub repository URL
      branch: main                # Branch to sync
      token: ""                   # GitHub token (use env var)
      retry:
        max-attempts: 3
        delay-ms: 1000
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SWAGGERDOCS_GIT_REMOTE_ENABLED` | Enable remote sync | `false` |
| `SWAGGERDOCS_GIT_REMOTE_URL` | GitHub repo URL | - |
| `SWAGGERDOCS_GIT_REMOTE_TOKEN` | GitHub token | - |
| `SWAGGERDOCS_GIT_REMOTE_BRANCH` | Branch name | `main` |

## API Endpoints

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/swaggers` | Submit a new swagger spec |
| `GET` | `/api/swaggers` | List all applications |
| `GET` | `/api/swaggers/{app}` | Get app info |
| `GET` | `/api/swaggers/{app}/raw` | Get raw OpenAPI spec |
| `GET` | `/api/swaggers/{app}/versions` | Get version history |
| `GET` | `/api/swaggers/{app}/diff` | Compare versions |

### Web Portal

| Route | Description |
|-------|-------------|
| `/` | Home - list all APIs |
| `/docs/{app}` | View API documentation |
| `/docs/{app}/history` | Version history |
| `/docs/{app}/diff` | Compare versions |

## Publishing from CI/CD

### GitHub Actions

Add this to your microservice's `.github/workflows/publish-swagger.yml`:

```yaml
name: Publish Swagger

on:
  push:
    branches: [main]
    paths:
      - 'src/main/resources/openapi/**'
      - '**/swagger*.yaml'
      - '**/swagger*.json'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Publish to SwaggerDocs
        run: |
          curl -X POST "${{ secrets.SWAGGERDOCS_URL }}/api/swaggers" \
            -H "Content-Type: application/json" \
            -d @- << EOF
          {
            "appName": "${{ github.event.repository.name }}",
            "team": "your-team",
            "environment": "production",
            "swagger": $(cat src/main/resources/openapi/api.json),
            "metadata": {
              "commitHash": "${{ github.sha }}",
              "branch": "${{ github.ref_name }}",
              "pipelineUrl": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
            }
          }
          EOF
```

## Supported Formats

The portal accepts multiple OpenAPI/Swagger specification versions:

| Format | Version Field | Schemas Location |
|--------|---------------|------------------|
| Swagger 2.0 | `"swagger": "2.0"` | `definitions/` |
| OpenAPI 3.0 | `"openapi": "3.0.x"` | `components/schemas/` |
| OpenAPI 3.1 | `"openapi": "3.1.x"` | `components/schemas/` |

## Submission Format

```json
{
  "appName": "my-api",
  "team": "backend-squad",
  "environment": "production",
  "swagger": { /* Swagger 2.0 or OpenAPI 3.x spec */ },
  "metadata": {
    "commitHash": "abc1234",
    "branch": "main",
    "pipelineUrl": "https://github.com/..."
  }
}
```

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `appName` | string | Unique app identifier (letters, numbers, hyphens, underscores) |
| `team` | string | Team responsible for the API |
| `swagger` | object | The OpenAPI/Swagger specification |

### Optional Fields

| Field | Type | Description |
|-------|------|-------------|
| `environment` | string | Deployment environment (e.g., production, staging) |
| `metadata.commitHash` | string | Git commit hash |
| `metadata.branch` | string | Git branch name |
| `metadata.pipelineUrl` | string | CI/CD pipeline URL |

## Error Responses

The API returns structured error responses for validation failures:

```json
// 400 Bad Request - Validation Error
{
  "timestamp": "2026-01-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    "appName: appName is required",
    "swagger: swagger is required"
  ]
}

// 400 Bad Request - Malformed JSON
{
  "timestamp": "2026-01-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Malformed JSON request"
}
```

## Quality Scoring

The portal validates your API spec and calculates a quality score (0-100):

| Category | Weight | Checks |
|----------|--------|--------|
| Descriptions | 25% | Operation summaries and descriptions |
| Examples | 20% | Request/response examples |
| Responses | 20% | Error responses (4xx, 5xx) |
| Schemas | 20% | Schema definitions and references |
| Metadata | 15% | Info, contact, license |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Controllers                               │
│  SwaggerController (/api/swaggers) - REST API               │
│  PortalController (/, /docs/{app}) - Web UI                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    SwaggerService                            │
│  Orchestrates validation, diff, and storage                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ GitStorage    │  │ Validation    │  │ DiffService   │
│ Service       │  │ Service       │  │               │
│ (JGit)        │  │ (Quality)     │  │ (Breaking)    │
└───────────────┘  └───────────────┘  └───────────────┘
```

## Development

```bash
# Run tests
mvn test

# Run single test
mvn test -Dtest=GitStorageServiceTest

# Build JAR
mvn package

# Run with debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug"
```

## License

MIT
