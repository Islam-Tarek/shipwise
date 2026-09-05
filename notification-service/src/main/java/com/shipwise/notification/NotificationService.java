package com.shipwise.notification;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Jdbi jdbi;

    NotificationService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Notification send(SendNotificationRequest request) {
        long id = jdbi.withExtension(NotificationDao.class, dao ->
                dao.insert(
                        request.type() == null ? "GENERIC" : request.type(),
                        request.recipient().trim().toLowerCase(),
                        request.message(),
                        request.channel().toLowerCase()));
        String recipient = request.recipient().trim().toLowerCase();
        log.info("queued notification {} ({} -> {})", id, request.type(), recipient);
        return new Notification(id, request.type(), recipient,
                "sent via " + request.channel() + " (mock delivery, RabbitMQ in phase 4)",
                request.channel(), java.time.Instant.now());
    }
}