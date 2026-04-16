# CI/CD Pipeline Documentation

## Overview
The Gayakini CI/CD pipeline is designed for high reliability and consistent quality. It uses GitHub Actions to automate the validation and deployment process.

## Pipeline Architecture

The pipeline consists of two primary jobs defined in `.github/workflows/ci.yml`:

### 1. Validation Gate (`validate`)
Runs on every `push` and `pull_request` to `main` and `develop`.
- **Environment**: `ubuntu-latest`, JDK 17 (Temurin).
- **Execution**: `./gradlew ciBuild`.
- **Steps**:
  1. **KtLint**: Validates code style against project standards.
  2. **Detekt**: Performs static code analysis to catch common Kotlin bugs and smells.
  3. **Tests**: Executes the full suite of unit and integration tests.
  4. **Kover**: Verifies code coverage against the current baseline (35%).
  5. **BootJar**: Assembles the final executable application JAR.
- **Artifacts**: Uploads the Kover HTML report for every run, regardless of success or failure.

### 2. Build & Publish (`publish`)
Runs only on `push` to the `main` branch, after the `validate` job succeeds.
- **Registry**: GitHub Container Registry (`ghcr.io`).
- **Images**: `ghcr.io/gayakini/gayakini`.
- **Tags**:
  - `latest`: The most recent successful build on `main`.
  - `sha-{commit-hash}`: Unique tag for every build, enabling precise rollbacks.
- **Security**: The build uses `Dockerfile` which also runs `ciBuild` internally to ensure no compromised or broken code is ever containerized.

## Local Replication
Developers can replicate the exact CI validation logic locally by running:
```bash
./gradlew ciBuild
```
This is the **Canonical Gate** that must pass before any pull request is opened.

## Quality Gates
The pipeline will fail if any of the following occur:
- Any `ktlint` violations.
- Any `detekt` issues (above the configured threshold).
- Any failing unit or integration tests.
- Code coverage falls below the current mandatory baseline (35%).
- The Spring Boot application fails to package.
