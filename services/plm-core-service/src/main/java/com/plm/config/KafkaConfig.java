package com.plm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plm.dto.ConversionMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    public static final String CONVERSION_TOPIC = "plm.conversion";
    public static final String ITEM_EVENTS_TOPIC = "plm.item-events";

    @Bean
    public NewTopic conversionTopic() {
        return TopicBuilder.name(CONVERSION_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic itemEventsTopic() {
        return TopicBuilder.name(ITEM_EVENTS_TOPIC).partitions(1).replicas(1).build();
    }
}
