package com.plm.workflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String WORKFLOW_EVENTS_TOPIC = "plm.workflow-events";

    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name(WORKFLOW_EVENTS_TOPIC).partitions(1).replicas(1).build();
    }
}
