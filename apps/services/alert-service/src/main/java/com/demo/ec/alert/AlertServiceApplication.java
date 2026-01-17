package com.demo.ec.alert;

import com.demo.ec.alert.config.AlertKafkaTopicsProperties;
import com.demo.ec.alert.config.AlertRulesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafka
@EnableKafkaStreams
@EnableConfigurationProperties({AlertKafkaTopicsProperties.class, AlertRulesProperties.class})
public class AlertServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
