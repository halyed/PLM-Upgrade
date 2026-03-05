package com.plm.integration.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // Topics this service consumes
    public static final String ITEM_EVENTS_TOPIC      = "plm.item-events";
    public static final String WORKFLOW_EVENTS_TOPIC  = "plm.workflow-events";

    // Topic this service publishes to (external → PLM)
    public static final String EXTERNAL_EVENTS_TOPIC  = "plm.external-events";

    @Bean
    public NewTopic externalEventsTopic() {
        return TopicBuilder.name(EXTERNAL_EVENTS_TOPIC).partitions(1).replicas(1).build();
    }
}
