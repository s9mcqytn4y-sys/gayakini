# ADR 0001: Sandbox-First Midtrans Strategy

## Status
Proposed

## Context
We are modernizing the Gayakini payment integration. The goal is to move towards a robust, E2E Midtrans Snap integration while maintaining high security and preventing accidental production deployments during the development phase.

## Decision
1. **Midtrans Snap Sandbox First**: Midtrans Snap will be the sole payment integration for this phase. All development and testing must happen against the Midtrans Sandbox environment.
2. **Biteship Schema-Only**: We will prepare the database schema for Biteship integration but will not make any external API calls to Biteship yet.
3. **Localhost-First Testing**: Initial E2E testing will be done on localhost, using simulated webhooks where necessary.
4. **Environment Lockdown**: We will enforce a strict `sandbox` profile. The application will fail to start if it detects production-like Midtrans configurations (e.g., `is-production: true` or production URLs) when the `sandbox` profile is active.

## Consequences
- Developers must use Sandbox keys.
- Accidental use of production keys in the sandbox environment is blocked at the application startup level.
- Clear separation between sandbox and production configurations.
