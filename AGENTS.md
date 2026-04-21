# AGENTS.md - Attribute Service

## Project Overview

Attribute Service provides CRUD operations for attributes in Hypertrace. An attribute is a granular piece of information that encapsulates metadata like data type, aggregation rules, and data sources. This service is part of the Hypertrace query architecture and stores/retrieves attribute metadata from MongoDB to assist both UI and backend in querying and displaying data generically.

This is a multi-module Gradle project using Kotlin DSL for build configuration.

## Build System

- **Build tool**: Gradle (Kotlin DSL)
- **Build all**: `./gradlew build`
- **Build Docker images**: `./gradlew dockerBuildImages`
- **Clean build**: `./gradlew clean build`

## Testing

- **Run all tests**: `./gradlew test`
- **Run integration tests**: `./gradlew integrationTest`
- **Run tests in specific module**: `./gradlew :module-name:test`
- **Run specific test class**: `./gradlew test --tests com.example.ClassName`

## Linting & Formatting

- **Check code style**: `./gradlew spotlessCheck`
- **Auto-fix code style**: `./gradlew spotlessApply`
- **Run before committing**: Always run `./gradlew spotlessApply build`

## Git Workflow

- **Branch naming**: `JIRA-TICKET-short-description` or `NO-TICKET-short-description`
- **Commit format**: `JIRA-TICKET: Description` or `NO-TICKET: Description`
- **PR title format**: Same as commit format
- **Default branch**: `main`

## Technology Stack

- **Language**: Java
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: MongoDB (for attribute metadata storage)
- **Framework**: Hypertrace platform
- **Deployment**: Docker containers with Helm charts

## Key Features

- **Attribute Metadata Management**: Store and retrieve attribute definitions including type, display name, groupability, sources, and scope
- **Seeded Attributes**: Initial attributes are seeded from `helm/configs` at startup
- **Dynamic Registration**: Attributes can be dynamically registered via APIs
- **Query Integration**: Integrates with hypertrace-graphql and gateway-service for query operations
- **Multi-backend Support**: Attributes include tags to locate services hosting their data (e.g., Query Service, Entity Service)

## DOs

- Follow existing code patterns in the codebase (immutability, builders, Lombok annotations)
- Run `./gradlew spotlessApply build` before committing to ensure code style compliance
- Use descriptive commit messages with Jira ticket references (or NO-TICKET prefix)
- Add unit tests for all new business logic
- Follow Java code style guidelines from the parent codebase (activity-event-service)
- Use `@Value`, `@Builder`, `@AllArgsConstructor` Lombok annotations for immutable data classes
- Make variables `final` wherever possible
- Use unmodifiable collections for return values

## DON'Ts

- Never force push to `main`
- Never commit secrets, `.env` files, or credentials
- Never skip git hooks (`--no-verify`)
- Don't modify attribute schemas without coordination (shared across services)
- Don't commit without running tests (`./gradlew test`)
- Don't commit without running code formatting (`./gradlew spotlessApply`)
- Don't break backward compatibility in attribute APIs without migration plan

## Commands to Never Run

- `git push --force origin main`
- `git commit --no-verify` or `git push --no-verify` (never skip hooks)
- `rm -rf /` (or any destructive recursive delete)

## Important Patterns

### Attribute Structure

Attributes contain key information:
- `key`: Unique identifier (e.g., "apiName")
- `value_kind`: Data type (TYPE_STRING, TYPE_INT64, etc.)
- `groupable`: Whether attribute supports grouping
- `display_name`: Human-readable name for UI
- `scope`: Context where attribute applies (API_TRACE, SERVICE, etc.)
- `sources`: Backend services that provide this data (QS = Query Service, ES = Entity Service)
- `type`: Classification (ATTRIBUTE, METRIC, etc.)

### Seeded vs Dynamic Attributes

- **Seeded**: Initial attributes loaded from `helm/configs` at service startup
- **Dynamic**: Attributes registered at runtime via API calls

## Development Workflow

1. Create a branch following the naming convention
2. Make your changes in the appropriate module(s)
3. Run `./gradlew spotlessApply` to format code
4. Run `./gradlew test` to verify tests pass
5. Commit with proper message format
6. Create a PR with the same title format

## Additional Resources

- See parent repository (activity-event-service) for detailed Java coding standards
- Docker images: [DockerHub > Attribute Service](https://hub.docker.com/r/hypertrace/attribute-service)
- Part of the Hypertrace Query Architecture
- Proto definitions: `attribute-service-api/src/main/proto/`
