#!/usr/bin/env bash
set -euo pipefail

docker stop shipwise-postgres
echo "postgres stopped, data kept in volume shipwise-pgdata (use reset-db.sh to wipe)"
