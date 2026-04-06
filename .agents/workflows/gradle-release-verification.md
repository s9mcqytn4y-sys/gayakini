---
description: Full Gradle release verification — clean + ktlint + detekt + test + build
---

# Gradle Release Verification

Runs the full quality gate chain for the gayakini backend.
Equivalent of the VSCode task "Gradle: release verification".

## Steps

// turbo-all

1. Clean build outputs:
```shell
.\gradlew.bat clean --console=plain
```

2. Run ktlint (advisory — `ignoreFailures=true` in build config):
```shell
.\gradlew.bat ktlintCheck --console=plain
```

3. Run detekt static analysis (advisory — `ignoreFailures=true` in build config):
```shell
.\gradlew.bat detekt --console=plain
```

4. Run tests:
```shell
.\gradlew.bat test --console=plain
```

5. Build the application:
```shell
.\gradlew.bat build --console=plain
```

Note: Steps 2-3 are currently advisory quality gates (repo has historical debt being ratcheted). Steps 4-5 are hard gates.
