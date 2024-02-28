package org.example.filemonitoringapi;

import org.example.filemonitoringapi.properties.RabbitSenderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(RabbitSenderProperties.class)
public class FileMonitoringApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileMonitoringApiApplication.class, args);
    }

}
