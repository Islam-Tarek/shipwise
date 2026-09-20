package com.shipwise.notification;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final Jdbi jdbi;
    private final JavaMailSender mailSender;
    private final String mailFrom;

    NotificationService(Jdbi jdbi,
                        JavaMailSender mailSender,
                        @Value("${custom.mail.from}") String mailFrom) {
        this.jdbi = jdbi;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    public Notification send(SendNotificationRequest request) {
        String recipient = request.recipient().trim().toLowerCase();
        String type = request.type() == null ? "GENERIC" : request.type();
        String channel = request.channel().toLowerCase();
        String message = request.message();

        String delivery;
        if ("email".equals(channel)) {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(recipient);
            mail.setSubject(type);
            mail.setText(message);
            mailSender.send(mail);
            delivery = "sent via email (SMTP)";
            log.info("sent {} email to {}", type, recipient);
        } else {
            delivery = "recorded for channel " + channel;
        }

        long id = jdbi.withExtension(NotificationDao.class, dao -> dao.insert(type, recipient, message, channel));
        log.info("notification {} recorded ({} -> {})", id, type, recipient);
        return new Notification(id, type, recipient, delivery, channel, java.time.Instant.now());
    }
}