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
- **Source of Truth**: The code is the source of truth. Generated OpenAPI from `/api-docs` is the canonical HTTP contract, and `docs/` must remain in sync.
- **ADRs**: Use `docs/adr/` for significant architectural decisions.
- **API Specs**: `docs/brand-fashion-ecommerce-api-final.yaml` is an archival snapshot, not the runtime contract.

## 5. Local Infrastructure
- **PostgreSQL**: Version 18+ required for production-parity local dev.
- **MCP Servers**: Use the provided PowerShell launchers in `tooling/mcp/` for automated workflows.

## 6. Project Status (Phase 5: Generated OpenAPI Baseline)
- [x] Centralized OpenAPI metadata + JWT bearer scheme
- [x] Generated `/api-docs` and `/swagger-ui.html` promoted as canonical API contract
- [x] Public, guest-token, and JWT-protected routes aligned for OpenAPI generation
- [x] Global exception handler hardened for validation/binding error payloads
- [x] Controller/DTO annotations hardened without using legacy Springfox
- [x] Docs updated to point integrators to generated OpenAPI truth
- [ ] Full runtime verification still depends on reachable PostgreSQL local environment

---
*Last Updated: 2026-04-13*
