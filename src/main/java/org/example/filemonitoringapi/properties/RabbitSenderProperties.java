package org.example.filemonitoringapi.properties;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "rabbit")
@Getter
@Setter
public class RabbitSenderProperties {
    private String mailQueueName;
}
