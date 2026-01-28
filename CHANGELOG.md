# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Multi-version OpenAPI/Swagger support (Swagger 2.0, OpenAPI 3.0, OpenAPI 3.1)
- OpenApiVersionDetector utility for spec version detection
- Jakarta Validation for request validation with proper error responses
- GlobalExceptionHandler for consistent error response formatting
- Validation annotations on SwaggerSubmission (@NotBlank, @NotNull, @Pattern)
- HttpMessageNotReadableException handler for malformed JSON requests
- Git remote sync support for Kubernetes deployments without persistent volumes
- GitRemoteConfig for configuring remote repository (URL, branch, token, retry settings)
- Automatic clone from remote on first startup
- Automatic pull from remote on subsequent startups
- Push with exponential backoff retry after each save operation
- GitSyncException for handling sync failures
- Integration tests for remote sync between multiple instances
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
