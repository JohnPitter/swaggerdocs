# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SwaggerDocs Portal - A centralized API documentation portal for enterprises. Applications send their OpenAPI/Swagger specs via CI pipelines, and this portal validates, versions, and displays them.

**Key Features:**
- Receives Swagger specs via REST API (POST /api/swaggers)
- Stores specs in local Git repository (JGit) for versioning
- Validates quality (descriptions, examples, error responses, schemas, metadata)
- Detects breaking changes between versions
- Displays docs using Swagger UI and Redoc

## Tech Stack

- Java 21, Spring Boot 3.2.x
- JGit for Git-based storage
- openapi-diff for breaking change detection
- Thymeleaf + WebJars (Swagger UI) for frontend
- Lombok for boilerplate reduction

## Build & Run Commands

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Run single test class
mvn test -Dtest=GitStorageServiceTest

# Run single test method
mvn test -Dtest=GitStorageServiceTest#shouldSaveAndRetrieveSwagger

# Run application
mvn spring-boot:run

# Package
mvn package
```

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

**Storage Structure** (in `~/.swaggerdocs/storage/`):
```
{app-name}/
├── swagger.json    # Current OpenAPI spec
└── metadata.json   # Team, commit, timestamp, etc.
```

## Key Patterns

- **Git as Database**: All swagger specs stored in local Git repo, versioning comes free
- **Two Constructors**: Services have Spring-managed constructor + test constructor (for mocking)
- **Quality Scoring**: Weighted categories (descriptions 25%, examples 20%, responses 20%, schemas 20%, metadata 15%)

## Implementation Plan

See `docs/plans/2026-01-26-swagger-portal.md` for detailed task breakdown with TDD steps.

---

## Development Principles

1. **Arquitetura Limpa**: Separação clara de camadas (controllers → services → repositories), dependências apontando para dentro
2. **Performance (Big O)**: Avaliar complexidade algorítmica, evitar O(n²) quando O(n) é possível, usar estruturas de dados apropriadas
3. **Segurança (CVEs)**: Mitigar OWASP Top 10, validar inputs, sanitizar outputs, dependências atualizadas
4. **Resiliência e Cache**: Circuit breakers, retries com backoff, cache onde faz sentido (Redis/local)
5. **Design Moderno**: UI/UX contextualizada ao domínio, responsivo, acessível
6. **Pirâmide de Testes**: Muitos testes unitários, alguns de integração, poucos E2E
7. **Proteção de Dados**: Sem logs de dados sensíveis, encryption at rest/transit, LGPD compliance
8. **Observabilidade**: Logs estruturados em todos os fluxos, métricas, traces (OpenTelemetry ready)
9. **Design System**: Componentes consistentes, tokens de design, documentação visual
10. **Construção por Fases**: Plano detalhado antes de implementar, subfases com entregas incrementais
11. **Changelog**: Toda alteração documentada em CHANGELOG.md seguindo Keep a Changelog
12. **Build Limpo**: Código compila sem warnings, imports não utilizados removidos, zero dead code

## Agent Behavior

1. **Timeout Management**: Comandos que demoram demais devem ser cancelados ou convertidos em background tasks
2. **Problem Solving**: Se uma solução não funcionar, pesquisar na internet e tentar abordagem alternativa
3. **Token Economy**: Foco na implementação, evitar resumos longos e explicações desnecessárias
