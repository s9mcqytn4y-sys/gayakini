#!/bin/bash
set -e

# Load .env if exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

case "$1" in
  infra-up)
    echo "Starting local infrastructure (DB/Mailpit)..."
    docker-compose -f docker-compose.yml up -d
    ;;
  infra-down)
    echo "Stopping local infrastructure..."
    docker-compose -f docker-compose.yml down
    ;;
  app-run)
    echo "Running app locally with profile: ${SPRING_PROFILES_ACTIVE:-local}"
    ./gradlew bootRun
    ;;
  dev-stack)
    echo "Starting full containerized dev stack..."
    docker-compose -f docker-compose.dev.yml up -d --build
    ;;
  dev-down)
    echo "Stopping dev stack..."
    docker-compose -f docker-compose.dev.yml down
    ;;
  staging-up)
    echo "Starting staging parity stack..."
    docker-compose -f docker-compose.staging.yml up -d --build
    ;;
  staging-down)
    echo "Stopping staging stack..."
    docker-compose -f docker-compose.staging.yml down
    ;;
  clean)
    ./gradlew clean
    docker system prune -f
    ;;
  *)
    echo "Usage: $0 {infra-up|infra-down|app-run|dev-stack|dev-down|staging-check|clean}"
    exit 1
esac
