/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.store.integration;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration and
 * Created by mike on 12/02/16.
 */
@Configuration
@IntegrationComponentScan
public class Exchanges {
    @Value("${org.fd.store.messaging.binding:fd.store.binding}")
    String storeBinding;

    @Value("${org.fd.store.messaging.exchange:fd.store.exchange}")
    String storeExchange;

    @Value("${org.fd.engine.binding:fd.engine.binding}")
    private String engineBinding;

    @Value("${org.fd.engine.messaging.exchange:fd.engine.exchange}")
    private String engineExchange;

    @Value("${org.fd.store.messaging.queue:fd.store.queue}")
    private String storeQueue;

    @Value("${org.fd.store.messaging.concurrentConsumers:2}")
    private int storeConcurrentConsumers;

    @Value("${org.fd.store.messaging.prefetchCount:3}")
    private int storePreFetchCount;

    @Value("${org.fd.store.messaging.dlq.queue:fd.store.dlq.queue}")
    private String storeDlq;

    @Value("${org.fd.store.messaging.dlq.exchange:fd.store.dlq.exchange}")
    private String storeDlqExchange;

    @Value("${org.fd.engine.messaging.dlq.exchange:fd.engine.dlq.exchange}")
    private String engineDlqExchange;

    @Value("${org.fd.engine.messaging.queue:fd.engine.queue}")
    private String engineQueue;

    String engineBinding() {
        return engineBinding;
    }

    String engineExchangeName(){
        return engineExchange;
    }
    int storeConcurrentConsumers() {
        return storeConcurrentConsumers;
    }

    int storePreFetchCount() {
        return storePreFetchCount;
    }

    @Bean
    Queue fdStoreQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", storeDlqExchange);
        return new Queue(storeQueue, true, false, false, params);
    }

    @Bean
    Queue fdEngineQueue(){
        Map<String,Object>params = new HashMap<>();
        params.put("x-dead-letter-exchange", engineDlqExchange);
        return new Queue(engineQueue, true, false, false, params);

    }

    @Bean
    Exchange fdEngineExchange() {
        return new DirectExchange(engineExchangeName());
    }

    @Bean
    Queue fdStoreQueueDlq(){
        return new Queue(storeDlq);
    }

    @Bean
    Exchange fdStoreExchange() {
        return new DirectExchange(storeExchange);
    }

    @Bean
    Exchange fdStoreDlqExchange() {
        return new DirectExchange(storeDlqExchange);
    }

}
