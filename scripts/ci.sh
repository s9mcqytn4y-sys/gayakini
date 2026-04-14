#!/bin/bash
set -e

# Gayakini Phase 6 CI Orchestrator
# This script mimics the GitHub Actions pipeline for local validation.

echo "🚀 Starting Gayakini CI Pipeline..."

# 1. Linting (ktlint)
echo "🔍 Running Linting (ktlint)..."
./gradlew ktlintCheck

# 2. Static Analysis (detekt)
echo "🛡️ Running Static Analysis (detekt)..."
./gradlew detekt

# 3. Unit & Isolated Tests (No Docker required)
echo "🧪 Running Fast Tests (Unit + Isolated)..."
./gradlew test -PexcludeIntegration

# 4. Coverage Report
echo "📊 Generating Coverage Report..."
./gradlew koverHtmlReport

echo "✅ CI Pipeline Passed Successfully!"
