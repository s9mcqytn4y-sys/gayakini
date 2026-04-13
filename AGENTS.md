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

## 🏢 Domain Knowledge & RBAC
The system handles complex data management and standardization for Indonesian industrial suppliers.
* **Roles:** The system implements a 4-role RBAC model: `CUSTOMER`, `ADMIN`, `FINANCE`, and `OPERATOR`.
* **Data Processing:** Interactions with structured JSON objects and Excel data formats.
* **Standardization:** Use centimeter (cm) standards unless specified otherwise.
* **Security:** All `/v1/admin/**` routes require `ADMIN` role. Finance and Operations have dedicated scopes.

## 📝 Coding Guidelines
1. **Kotlin Idioms:** Utilize Kotlin's modern features (null safety, extension functions, data classes) to keep the code concise and safe.
2. **Immutability:** Prefer `val` over `var` and immutable collections by default.
3. **Clean Architecture:** Keep business logic decoupled from framework-specific configurations.
4. **Quality Tools:** ktlint (12.1.1) and detekt (1.23.8) are enforced via Gradle.
