#!/usr/bin/env bash
set -euo pipefail

docker stop shipwise-temporal
echo "temporal dev server stopped, history kept in ./data/temporal/temporal.db"