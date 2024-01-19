package org.example.filemonitoringapi.subscription.model;

import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public Subscription fromCreateCommand(CreateSubscriptionCommand command) {
        return Subscription.builder()
                .filePath(command.getFilePath())
                .email(command.getEmail())
                .build();
    }

    public SubscriptionDto toDTO(Subscription subscription) {
        return SubscriptionDto.builder()
                .filePath(subscription.getFilePath())
                .email(subscription.getEmail())
                .jobId(subscription.getJobId())
                .active(subscription.isActive())
                .build();
    }
}
