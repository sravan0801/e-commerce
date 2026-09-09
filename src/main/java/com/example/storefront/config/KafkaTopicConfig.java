package com.example.storefront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * The one topic this app uses. Declared as a bean so Spring creates it on
 * startup instead of relying on broker auto-creation.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String ORDERS_PLACED = "orders.placed";

    @Bean
    public NewTopic ordersPlacedTopic() {
        return TopicBuilder.name(ORDERS_PLACED).partitions(1).replicas(1).build();
    }
}
