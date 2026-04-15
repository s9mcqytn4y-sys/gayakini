#!/bin/bash
set -e

# Gayakini CI Orchestrator
# This script mimics the GitHub Actions pipeline for local validation.

echo "🚀 Starting Gayakini CI Pipeline (Local Synchronization)..."

# Use the canonical Gradle task for full verification
# This includes ktlint, detekt, all tests (Unit + Integration), and Kover coverage gate.
./gradlew ciBuild

echo "✅ CI Pipeline Passed Successfully!"
echo "📊 Coverage report available at: build/reports/kover/html/index.html"
