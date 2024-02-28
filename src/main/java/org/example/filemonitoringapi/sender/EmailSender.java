package org.example.filemonitoringapi.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.filemonitoringapi.email.EmailDetails;
import org.example.filemonitoringapi.properties.RabbitSenderProperties;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitSenderProperties rabbitSenderProperties;

    public void sendDetails(EmailDetails emailDetails) {
        try {
            rabbitTemplate.convertAndSend(rabbitSenderProperties.getMailQueueName(), emailDetails);
        } catch (AmqpException e) {
            log.error("Nie udało się wysłać szczegółów e-maila przez RabbitMQ", e);

        }
    }

}
