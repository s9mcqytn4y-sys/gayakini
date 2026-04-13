# Testing Strategy

## Overview
We prioritize a stable and reliable test suite using JUnit 5. All tests are executed via the Gradle `test` task.

## Test Types
1. **Unit Tests**: Focus on business logic in isolation.
2. **Integration Tests**: Focus on the interaction between components, including Spring context and database interactions (using H2 for tests).

## Running Tests
- **All Tests**: `./gradlew test`
- **Single Test Class**: `./gradlew test --tests "com.gayakini.package.ClassName"`
- **Quality Gate**: `./gradlew qualityGate` (includes tests and static analysis)

## Test Configuration
- **JUnit Platform**: All tests use the JUnit Platform.
- **Test Logging**: Failed tests will display full stack traces in the console. Standard streams (stdout/stderr) are shown.

## Best Practices
- **Naming**: Use descriptive names for test methods.
- **Immutability**: Tests should not share state.
- **Assertions**: Use Spring Boot Starter Test's built-in assertion libraries.
