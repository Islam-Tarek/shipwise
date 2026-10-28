#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

REMOVE_VOLUMES=false
if [ "${1:-}" = "--volumes" ]; then
  REMOVE_VOLUMES=true
fi

echo "================================================"
echo "  Shipwise - Docker Compose Shutdown"
echo "================================================"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker is not installed or not in PATH"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker daemon is not running"
  exit 1
fi

if $REMOVE_VOLUMES; then
  echo "WARN: This will DELETE all persisted data (Postgres, Temporal, LGTM)."
  read -rp "Are you sure? (yes/no): " confirm
  if [ "$confirm" != "yes" ]; then
    echo "Aborted."
    exit 0
  fi
  docker compose down -v
else
  docker compose down
fi

echo ""
echo "================================================"
echo "  Stack stopped"
echo "================================================"
if $REMOVE_VOLUMES; then
  echo "  Volumes removed."
else
  echo "  Volumes preserved. Use --volumes to delete data."
fi
echo "================================================"