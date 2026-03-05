package com.plm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PlmApplicationTests {

    // Prevents Spring from trying to contact Keycloak during context load
    @MockBean
    JwtDecoder jwtDecoder;

    // Prevents Spring from needing a real Kafka broker for DocumentService
    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
