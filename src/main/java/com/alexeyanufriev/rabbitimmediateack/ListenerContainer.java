package com.alexeyanufriev.rabbitimmediateack;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.ImmediateAcknowledgeAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ListenerContainer {

    @RabbitListener(queues = "test-queue", containerFactory = "listener-factory")
    void listener(Message message, Channel channel) {
        System.out.println("# Received <" + new String(message.getBody()) + ">");
        throw new ImmediateAcknowledgeAmqpException("Immediate Ack");
    }

}
