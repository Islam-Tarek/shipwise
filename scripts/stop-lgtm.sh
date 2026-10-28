#!/usr/bin/env bash
set -euo pipefail

CONTAINER="shipwise-lgtm"
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  docker stop "${CONTAINER}" >/dev/null
  echo "stopped ${CONTAINER}"
else
  echo "${CONTAINER} not running"
fi
