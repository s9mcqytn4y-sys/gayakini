---
description: Full Gradle release verification — doctor + lint + detekt + test + flyway + mcp
---

# Gradle Release Verification

Runs the full quality gate chain for the gayakini backend using the `releaseCheck` task.
Equivalent of the VSCode task "Gradle: releaseCheck".

## Steps

// turbo-all

1. Run the full release gate:
```shell
.\gradlew.bat releaseCheck --console=plain
```

This task includes:
- `dbVerify`: Read-only diagnostics for database connectivity.
- `ktlintCheck`: Code style enforcement (**Blocking**).
- `detekt`: Static analysis (**Blocking**).
- `assemble`: Build the application artifact.
- `validateMcp`: MCP launcher preflight.

Note: All gates are now **blocking**. Failure in any step will prevent the release from being considered compliant.
