#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
echo "stopping and removing container + volume, all data will be lost"
docker rm -f shipwise-postgres 2>/dev/null || true
docker volume rm shipwise-pgdata 2>/dev/null || true
./start-postgres.sh