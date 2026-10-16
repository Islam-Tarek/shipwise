package com.shipwise.order.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shipwise.order.rabbit.OrderEventsConfig;

@Component
public class OrderOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxDispatcher.class);
    private static final int BATCH = 50;
    private static final long CONFIRM_TIMEOUT_MS = 3000;

    private final Jdbi jdbi;
    private final RabbitTemplate rabbitTemplate;
    private final ObservationRegistry observationRegistry;

    OrderOutboxDispatcher(Jdbi jdbi, RabbitTemplate rabbitTemplate, ObservationRegistry observationRegistry) {
        this.jdbi = jdbi;
        this.rabbitTemplate = rabbitTemplate;
        this.observationRegistry = observationRegistry;
    }

    @Scheduled(fixedDelay = 1000)
    public void flush() {
        List<OrderOutbox> rows = jdbi.withExtension(OrderOutboxDao.class, dao -> dao.claimUnsent(BATCH));
        if (rows.isEmpty()) {
            return;
        }
        Observation flushObs = Observation.createNotStarted("outbox.flush", observationRegistry)
                .contextualName("outbox.flush")
                .lowCardinalityKeyValue("outbox.batch.size", String.valueOf(rows.size()));
        flushObs.observe(() -> {
            for (OrderOutbox row : rows) {
                try {
                    Observation publishObs = Observation.createNotStarted("outbox.publish", observationRegistry)
                            .contextualName("outbox.publish")
                            .lowCardinalityKeyValue("messaging.system", "rabbitmq")
                            .lowCardinalityKeyValue("messaging.operation", "publish")
                            .lowCardinalityKeyValue("messaging.destination.exchange", OrderEventsConfig.EXCHANGE)
                            .lowCardinalityKeyValue("messaging.destination.name", OrderEventsConfig.QUEUE)
                            .lowCardinalityKeyValue("messaging.rabbitmq.destination.routing_key", OrderEventsConfig.ROUTING_KEY)
                            .lowCardinalityKeyValue("event.type", row.eventType())
                            .lowCardinalityKeyValue("order.id", String.valueOf(row.orderId()))
                            .lowCardinalityKeyValue("outbox.row_id", String.valueOf(row.id()));
                    publishObs.observeChecked(() -> {
                        publish(row);
                        return null;
                    });
                    jdbi.useExtension(OrderOutboxDao.class, dao -> dao.markSent(row.id()));
                    log.info("outbox: published {} for order {} -> {}", row.eventType(), row.orderId(),
                            OrderEventsConfig.ROUTING_KEY);
                } catch (Exception e) {
                    jdbi.useExtension(OrderOutboxDao.class, dao -> dao.markFailed(row.id()));
                    log.warn("outbox: publish failed for row {} (order {}, attempts now {}) - will retry next tick: {}",
                            row.id(), row.orderId(), row.attempts() + 1, e.getMessage());
                }
            }
        });
    }

    private void publish(OrderOutbox row) throws Exception {
        CorrelationData correlation = new CorrelationData(row.id() + "-" + row.orderId());
        rabbitTemplate.convertAndSend(OrderEventsConfig.EXCHANGE, OrderEventsConfig.ROUTING_KEY,
                row.payload(), correlation);
        if (correlation.getFuture() == null) {
            throw new IllegalStateException("publisher confirms not configured (publisher-confirm-type=correlated)");
        }
        var confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            throw new IllegalStateException("broker nack/timeout for row " + row.id()
                    + (confirm == null ? "" : " reason=" + confirm.getReason()));
        }
        if (correlation.getReturned() != null) {
            throw new IllegalStateException("unroutable message for row " + row.id() + " -> "
                    + correlation.getReturned().getReplyText());
        }
    }
}