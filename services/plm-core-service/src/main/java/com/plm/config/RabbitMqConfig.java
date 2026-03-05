package com.plm.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CONVERSION_QUEUE    = "plm.conversion";
    public static final String CONVERSION_EXCHANGE = "plm.conversion.exchange";
    public static final String CONVERSION_KEY      = "conversion";

    @Bean
    public Queue conversionQueue() {
        return QueueBuilder.durable(CONVERSION_QUEUE).build();
    }

    @Bean
    public DirectExchange conversionExchange() {
        return new DirectExchange(CONVERSION_EXCHANGE);
    }

    @Bean
    public Binding conversionBinding(Queue conversionQueue, DirectExchange conversionExchange) {
        return BindingBuilder.bind(conversionQueue).to(conversionExchange).with(CONVERSION_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);
        return tpl;
    }
}
