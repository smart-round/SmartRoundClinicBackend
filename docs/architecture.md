# SmartRoundClinic Architecture

## Overview
SmartRoundClinic is built using a **Multi-Module Clean Architecture** with **Ktor**. The project is divided into several independent modules, each following Clean Architecture principles internally.

## Modules
- `:admin`: Admin-specific business logic and REST endpoints.
- `:doctor`: Doctor-specific business logic and REST endpoints.
- `:patient`: Patient-specific business logic and REST endpoints.
- `:notification`: Responsible for sending emails and push notifications (No REST endpoints).
- `:auth`: Handles user signup, login, and authentication business logic.
- `:infra`: Infrastructure layer (Ktor plugins, JWT verification, MongoDB setup, Koin configuration).
- `:common`: Shared utilities, base DTOs, enums, and constants.

## Internal Module Architecture (Clean Architecture)
Each functional module (`admin`, `:doctor`, `:patient`, `:auth`) is organized into the following layers:

### Data Layer (`data/`)
- `entity/`: MongoDB documents or DAOs for data serialization.
- `source/`: Database connections or remote API clients.
- `repository/`: Implementation of domain interfaces.

### Domain Layer (`domain/`)
- `model/`: Business data classes (POJOs).
- `repository/`: Interface definitions for the data layer.
- `usecase/`: Single responsibility business logic classes.
- `service/`: Facade binding use cases for injection into the presentation layer.

### Presentation Layer (`presentation/`)
- `controller/`: Ktor routing and request handling.
- `dto/`: Request and Response DAOs.
    - `request/`: Classes for incoming JSON.
    - `response/`: Classes for outgoing JSON.

## Dependency Rules
1. **Domain Layer** has no dependencies on other layers.
2. **Data Layer** depends on the Domain layer.
3. **Presentation Layer** depends on the Domain layer (Services/Use-cases).
4. **Presentation MUST NOT** depend on the Data layer.
5. All feature modules and `:infra` depend on `:common`.
6. Functional modules depend on `:infra` for cross-cutting concerns like security.

## Security & Authentication
- **Verification**: JWT token verification is handled in the `:infra` module.
- **Principal**: Feature modules extract the `JWTPrincipal` from the route using `call.principal<JWTPrincipal>()`.
- **Authorization**: Protected routes are wrapped in an `authenticate("auth-jwt")` block.

## Dependency Injection (Koin)
Each module defines its own Koin module to bind implementations to interfaces. These are then combined and started in the `:infra` layer or at the application entry point.
