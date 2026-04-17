#!/bin/bash
set -e

# Load .env if exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

case "$1" in
  infra-up)
    echo "Starting local infrastructure (DB/Mailpit)..."
    docker compose up -d
    ;;
  infra-down)
    echo "Stopping local infrastructure..."
    docker compose down
    ;;
  app-run)
    echo "Running app locally with profile: ${SPRING_PROFILES_ACTIVE:-local}"
    ./gradlew bootRun
    ;;
  dev-stack)
    echo "Starting full containerized dev stack..."
    docker compose --profile full up -d --build
    ;;
  dev-down)
    echo "Stopping dev stack..."
    docker compose --profile full down
    ;;
  clean)
    ./gradlew clean
    docker system prune -f
    ;;
  *)
    echo "Usage: $0 {infra-up|infra-down|app-run|dev-stack|dev-down|clean}"
    exit 1
esac
