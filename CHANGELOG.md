# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project setup with Spring Boot 3.2.2 and Java 21
- Domain models: SwaggerSubmission, SwaggerMetadata, ValidationResult, QualityScore, BreakingChange, SwaggerEntry, SwaggerInfo
- Git-based storage service with JGit for versioning swagger specs
- Validation service with weighted quality scoring (descriptions, examples, responses, schemas, metadata)
- Diff service for breaking change detection (removed endpoints, methods, schema properties)
- Swagger orchestration service
- REST API controller (`/api/swaggers`)
- Portal web controller with Thymeleaf templates
- Swagger UI and Redoc integration via WebJars
- Jackson datetime support with JSR310
- Integration tests for full workflow
- Maven wrapper for portable builds
- CLAUDE.md with project guidelines and development principles
