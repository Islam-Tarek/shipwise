package com.shipwise.notification.rabbit;

import java.math.BigDecimal;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.shipwise.common.event.OrderConfirmedEvent;
import com.shipwise.notification.NotificationDao;

import tools.jackson.databind.ObjectMapper;

@Component
public class OrderConfirmedEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedEventListener.class);

    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;
    private final Jdbi jdbi;
    private final String mailFrom;

    OrderConfirmedEventListener(ObjectMapper objectMapper,
                                JavaMailSender mailSender,
                                Jdbi jdbi,
                                @Value("${custom.mail.from}") String mailFrom) {
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.jdbi = jdbi;
        this.mailFrom = mailFrom;
    }

    @RabbitListener(queues = NotificationsRabbitConfig.QUEUE)
    public void onOrderConfirmed(String payload) throws Exception {
        OrderConfirmedEvent event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
        long orderId = event.orderId();

        boolean alreadyDelivered = jdbi.withExtension(NotificationDao.class, dao ->
                dao.countByOrderId(orderId)) > 0;
        if (alreadyDelivered) {
            log.info("order {} confirmation already delivered - skipping duplicate", orderId);
            return;
        }

        String body = buildBody(event);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailFrom);
        mail.setTo(event.customerEmail());
        mail.setSubject("ShipWise order #%d confirmed".formatted(orderId));
        mail.setText(body);
        mailSender.send(mail);

        jdbi.useExtension(NotificationDao.class, dao ->
                dao.insertForOrder(orderId, "ORDER_CONFIRMED", event.customerEmail().trim().toLowerCase(), body, "email"));
        log.info("order {} confirmation email sent to {}", orderId, event.customerEmail());
    }

    private String buildBody(OrderConfirmedEvent event) {
        StringBuilder sb = new StringBuilder("Hello ").append(event.customerName()).append(",\n\n")
                .append("Your ShipWise order #").append(event.orderId())
                .append(" has been confirmed.\n\nItems:\n");
        for (var item : event.items()) {
            sb.append("  - ").append(item.quantity()).append("x ").append(item.productName())
                    .append(" (").append(item.unitPrice()).append(" USD)\n");
        }
        sb.append("\nTotal: ").append(formatTotal(event.total())).append(" USD\n\n")
                .append("Thank you for shopping with ShipWise.");
        return sb.toString();
    }

    private String formatTotal(BigDecimal total) {
        return total == null ? "0.00" : total.toPlainString();
    }
}