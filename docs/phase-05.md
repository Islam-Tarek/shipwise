# Phase 5 notes — OpenTelemetry observability (OTLP/HTTP)

## What was built
Added end-to-end OpenTelemetry (traces, metrics, logs) to all eight services, exporting over OTLP/HTTP to grafana/otel-lgtm. Instrumentation includes manual spans for JDBI/transactions, the Temporal saga (workflow + activities) and the order outbox dispatcher, plus automatic spans for HTTP/RabbitMQ via Micrometer Observation. Logs are shipped via the OpenTelemetry Logback appender with trace/span correlation.

## Architecture
- Transport: OTLP/HTTP only (protobuf) on http://localhost:4318. Traces /v1/traces, metrics /v1/metrics, logs /v1/logs.
- Emission: spring-boot-starter-opentelemetry on every service; Boot auto-configures SdkTracerProvider/SdkLoggerProvider, Resource, OTLP exporters with protocol http/protobuf.
- Logging: logback-spring.xml + OpenTelemetry Logback appender (opentelemetry-logback-appender-1.0:2.21.0-alpha); OpenTelemetryLogbackInitializer installs appender against the OpenTelemetry bean.
- Messaging/HTTP: Micrometer Observation auto-instruments WebMVC/WebFlux and Spring AMQP; RabbitTemplate/listener observation enabled.
- JDBI + outbox: manual ObservationRegistry spans around confirmOrder transaction and outbox flush/publish (batch + per-row).
- Temporal: OpenTracing interceptors (via OpenTelemetry<->OpenTracing shim) registered on client/worker with tracer from OpenTracingShim.createTracerShim(openTelemetry).
- Backend: grafana/otel-lgtm (single container) provides Collector + Tempo + Mimir + Loki + Grafana (:3000), pre-provisioned datasources.

## Key changes
- Parent pom: added otel.logback.appender.version property.
- All service poms: added spring-boot-starter-opentelemetry and opentelemetry-logback-appender-1.0.
- order-service pom: added temporal-opentracing and opentelemetry-opentracing-shim (bridged to Boot OTel).
- shipwise-common: OpenTelemetryLogbackInitializer (provided-scoped OTel/logback deps).
- Per-service: logback-spring.xml; application.yml extended with management.opentelemetry.* (traces/metrics/logs OTLP HTTP endpoints) and rabbitmq observation flags.
- order-service: TemporalConfig uses OpenTracing interceptors with shim tracer; OrderSagaActivitiesImpl wraps confirmOrder in observation; OrderOutboxDispatcher wraps flush/publish with batch+per-row observations.
- Scripts: start-lgtm.sh, stop-lgtm.sh; dev.sh auto-starts LGTM if Docker available and not running.

## Verification
- Build: mvn clean install -DskipTests passes (all modules).
- LGTM: ./scripts/start-lgtm.sh starts container on :3000/:4317/:4318, Grafana ready.
- Services boot with OTel initialized; logs include trace/span correlation via OTEL appender.
- happy-path.sh + notification-test.sh (A+B) + kill-test.sh + last-unit-test.sh: behavior unchanged; traces/metrics/logs exported to LGTM.
