package com.shipwise.notification.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationsRabbitConfig {

    public static final String EVENTS_EXCHANGE = "shipwise.events";
    public static final String QUEUE = "order-notifications";
    public static final String ROUTING_KEY = "order.confirmed";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotificationsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding orderNotificationsBinding(Queue orderNotificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderNotificationsQueue).to(eventsExchange).with(ROUTING_KEY);
    }
}