# AI Agent Instructions for Gayakini Repository

## 🎯 Project Context
This repository contains the backend architecture for the **Gayakini** project. You are acting as an expert Software Developer and System Architect assisting with the "gayakini reset" initiative.

## 🛠️ Technology Stack & Constraints
When writing, refactoring, or reviewing code, strictly adhere to the following stack:
* **Language:** Kotlin 2.0.21
* **Framework:** Spring Boot 3.4.0
* **Runtime:** JDK 17
* **Database Migrations:** Flyway
* **Security:** Spring Security

*Do not suggest downgrading versions or introducing libraries outside of this core stack without explicit instruction.*

## 📍 Current Phase: Phase 5
The current primary objective (Phase 5) is focused on **automatic API contract generation** and **OpenAPI hardening**.
* Ensure all new controllers and endpoint modifications strictly align with OpenAPI specification standards.
* Pay close attention to request/response payload structures and validation constraints.

## ⚙️ DevOps & Terminal Workflows
To optimize context windows and reduce token consumption, terminal output noise must be kept to an absolute minimum.
* **Terminal Filtering:** Always assume the use of the custom PowerShell filtering proxy located at `tooling/rtk/filter-terminal-output.ps1`.
* When suggesting CLI commands for builds or tests, integrate them with the filtering script where applicable to ensure stdout/stderr output is clean and concise.

## 🏢 Domain Knowledge & Data Standards
The system handles complex data management and standardization for business entities, specifically focusing on Indonesian industrial suppliers (e.g., textile, carpet, and label manufacturing companies).
* **Data Processing:** Expect frequent interactions with structured JSON objects and Excel data formats.
* **Standardization:** When dealing with physical dimensions in the codebase or data structures, use centimeter (cm) standards unless specified otherwise.
* **Entity Validation:** Maintain strict validation for business entity names and related metadata.

## 📝 Coding Guidelines
1. **Kotlin Idioms:** Utilize Kotlin's modern features (null safety, extension functions, data classes, coroutines if applicable) to keep the code concise and safe.
2. **Immutability:** Prefer `val` over `var` and immutable collections by default.
3. **Clean Architecture:** Keep business logic decoupled from framework-specific configurations.
