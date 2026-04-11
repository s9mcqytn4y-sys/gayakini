# Agents Operational Contract: gayakini

This is the **canonical source of truth** for AI agents and developers working on this repository. It defines the operational guardrails, workflow expectations, and security mandates.

## 1. Core Mission
Build a production-grade, local-first e-commerce backend with "honest REST" and zero-trust security. The system must be robust enough for single-outlet retail but scalable in its modular-monolith design.

## 2. Security Mandate (Hard Requirements)
- **Stateless Auth**: Only JWT (jjwt 0.12+) is allowed. No sessions.
- **Secret Hygiene**: Minimum 256-bit (32 chars) secret for JWT.
- **Constant-Time Verification**: All cryptographic comparisons (signatures, hashes) must use `MessageDigest.isEqual()`.
- **RBAC**: Exactly two roles: `ADMIN` and `CUSTOMER`. Default to `deny-all` for new endpoints.
- **Webhook Hardening**: Signature verification is non-negotiable for all provider ingress.

## 3. Workflow & Tooling
- **Build System**: Gradle with Kotlin DSL.
- **Quality Gates**: `ktlint` and `detekt` are blocking. Run `./gradlew releaseCheck` before finishing tasks.
- **Testing**: JUnit 5 + MockMvc + H2 (for integration tests). Use `useJUnitPlatform()` in Gradle.
- **IDE Tools**: Use built-in specialized tools (`read_file`, `grep`, `find_declaration`) over shell commands whenever possible.

## 4. Documentation Strategy
- **Source of Truth**: The code is the source of truth, but `docs/` must remain in sync.
- **ADRs**: Use `docs/adr/` for significant architectural decisions.
- **API Specs**: `brand-fashion-ecommerce-api-final.yaml` is the contract.

## 5. Local Infrastructure
- **PostgreSQL**: Version 18+ required for production-parity local dev.
- **MCP Servers**: Use the provided PowerShell launchers in `tooling/mcp/` for automated workflows.

## 6. Project Status (Phase 6: Midtrans/Biteship SDK & Stabilization)
- [x] JWT Infrastructure (jjwt 0.12.6)
- [x] Route Whitelisting (SecurityConfig.kt)
- [x] Standardized 401/403 Responses
- [x] Constant-Time Signature Validation
- [x] Security Baseline Integration Tests
- [x] Enabled JUnit 5 Platform
- [x] H2 Test Profile Configuration
- [x] Midtrans Java SDK Integration (3.2.2)
- [x] Database Schema Alignment (Flyway V17)
- [x] Order Lifecycle & Inventory Integrity Tests
- [x] Quality Gates (ktlint/detekt) Passing

---
*Last Updated: 2026-04-11*
