package org.example.filemonitoringapi.model;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
@AllArgsConstructor
@Component
public class SubscriptionMapper {

    // Konwersja z CreateSubscriptionCommand na Subscription
    public  Subscription fromCreateCommand(CreateSubscriptionCommand command) {
        Subscription subscription = new Subscription();
        subscription.setFilePath(command.getFilePath());
        subscription.setEmail(command.getEmail());
        subscription.setJobId(command.getJobId());
        return subscription;
    }

    // Konwersja z Subscription na SubscriptionDto
    public  SubscriptionDto toDTO(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setFilePath(subscription.getFilePath());
        dto.setEmail(subscription.getEmail());
        dto.setJobId(subscription.getJobId());
        return dto;
    }
}
