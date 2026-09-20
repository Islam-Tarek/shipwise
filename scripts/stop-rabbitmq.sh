#!/usr/bin/env bash
set -euo pipefail

if docker ps --format '{{.Names}}' | grep -q '^shipwise-rabbitmq$'; then
  docker stop shipwise-rabbitmq
  echo "rabbitmq stopped"
else
  echo "rabbitmq not running"
fi