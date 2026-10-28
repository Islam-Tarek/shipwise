#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "================================================"
echo "  Shipwise - Docker Compose Stack Startup"
echo "================================================"

# 1. Check Docker is available
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker is not installed or not in PATH"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: Docker daemon is not running. Start Docker Desktop or daemon."
  exit 1
fi

# 2. Ensure .env exists
if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    echo "INFO: .env not found. Copying from .env.example ..."
    cp .env.example .env
    echo "WARN: Please edit .env with your Gmail app password before relying on email delivery."
  else
    echo "WARN: No .env or .env.example found. Notification emails may fail."
  fi
fi

# 3. Build and start
echo ""
echo "Building and starting all services (this may take several minutes on first run)..."
docker compose up -d --build

# 4. Wait for key services to be ready
echo ""
echo "Waiting for services to become ready..."

wait_for_port() {
  local host="$1"
  local port="$2"
  local name="$3"
  for i in $(seq 1 60); do
    if (echo > /dev/tcp/"$host"/"$port") 2>/dev/null; then
      echo "  [OK] $name ($host:$port) is ready"
      return 0
    fi
    sleep 2
  done
  echo "  [FAIL] $name ($host:$port) did not become ready within 2 minutes"
  return 1
}

wait_for_port localhost 5433 "PostgreSQL"
wait_for_port localhost 7233 "Temporal gRPC"
wait_for_port localhost 8233 "Temporal Web UI"
wait_for_port localhost 5672 "RabbitMQ AMQP"
wait_for_port localhost 15672 "RabbitMQ Management"
wait_for_port localhost 3000 "Grafana LGTM"
wait_for_port localhost 4318 "OTLP Collector"
wait_for_port localhost 9000 "Gateway"
wait_for_port localhost 8080 "Order Service"
wait_for_port localhost 8081 "Product Service"
wait_for_port localhost 8082 "Inventory Service"
wait_for_port localhost 8083 "Payment Service"
wait_for_port localhost 8084 "Shipping Service"
wait_for_port localhost 8085 "Notification Service"
wait_for_port localhost 8086 "Auth Service"

echo ""
echo "================================================"
echo "  Stack is ready"
echo "================================================"
echo "  Gateway:          http://localhost:9000"
echo "  Temporal UI:      http://localhost:8233"
echo "  RabbitMQ UI:      http://localhost:15672 (shipwise/shipwise)"
echo "  Grafana LGTM:     http://localhost:3000 (admin/admin)"
echo "  Storefront:       http://localhost:5173"
echo "  OTLP endpoint:    http://localhost:4318"
echo ""
echo "  Stop with: ./scripts/compose-down.sh"
echo "================================================"