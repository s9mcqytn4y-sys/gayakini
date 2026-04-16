# Release Checklist

This checklist ensures that every release to production meets the quality standards of the Gayakini platform.

## Pre-Release (Development)
- [ ] Feature complete and verified against requirements.
- [ ] Unit tests written for all new logic.
- [ ] Integration tests covering happy and edge paths.
- [ ] No regression in existing tests.
- [ ] `./gradlew ciBuild` passes locally.

## Release Process
- [ ] Merge `develop` into `main` via Pull Request.
- [ ] Verify GitHub Actions `CI/CD Pipeline` completes successfully.
- [ ] Confirm Docker image is published to GHCR with `latest` and `sha-*` tags.

## Post-Release (Production)
- [ ] Verify database migrations applied successfully (Flyway).
- [ ] Perform smoke tests on core flows (Login, Product Search, Checkout).
- [ ] Monitor logs for any unexpected spikes in errors.
- [ ] Verify `/actuator/health` returns `UP`.

## Rollback Plan
- [ ] If critical issues occur, redeploy the previous stable Docker image using its `sha-*` tag.
- [ ] Identify if a database rollback is required (Flyway `undo` or manual intervention).
