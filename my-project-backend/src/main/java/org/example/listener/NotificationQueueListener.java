package org.example.listener;

import jakarta.annotation.Resource;
import org.example.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationQueueListener {
    @Resource NotificationService service;
    @RabbitListener(queues = "notification")
    public void consume(NotificationService.NotificationEvent event) { service.consume(event); }
}
