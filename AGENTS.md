# AI Agent Instructions for Gayakini Repository

## 🎯 Project Context
This repository contains the backend architecture for the **Gayakini** project, an e-commerce platform for industrial suppliers.

## 🛠️ Technology Stack & Constraints
When writing, refactoring, or reviewing code, strictly adhere to the following stack:
* **Language:** Kotlin 2.0.21
* **Framework:** Spring Boot 3.4.0
* **Runtime:** JDK 17
* **Database Migrations:** Flyway
* **Security:** Spring Security
* **Database:** PostgreSQL
* **Payment Integration:** Midtrans

*Do not suggest downgrading versions or introducing libraries outside of this core stack without explicit instruction.*

## 🏢 Domain Knowledge & Data Standards
The system handles complex data management and standardization for business entities, specifically focusing on Indonesian industrial suppliers (e.g., textile, carpet, and label manufacturing companies).
* **Data Processing:** Interactions with structured JSON objects and Excel data formats.
* **Standardization:** When dealing with physical dimensions in the codebase or data structures, use centimeter (cm) standards unless specified otherwise.
* **Entity Validation:** Maintain strict validation for business entity names and related metadata.

## 📝 Coding Guidelines
1. **Kotlin Idioms:** Utilize Kotlin's modern features (null safety, extension functions, data classes) to keep the code concise and safe.
2. **Immutability:** Prefer `val` over `var` and immutable collections by default.
3. **Clean Architecture:** Keep business logic decoupled from framework-specific configurations.
4. **Quality Tools:** ktlint (12.1.1) and detekt (1.23.8) are enforced via Gradle.
