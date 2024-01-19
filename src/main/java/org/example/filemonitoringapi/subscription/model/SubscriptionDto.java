package org.example.filemonitoringapi.subscription.model;

import lombok.*;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class SubscriptionDto {
    private String filePath;
    private String email;
    private String jobId;
    private boolean active;
}
