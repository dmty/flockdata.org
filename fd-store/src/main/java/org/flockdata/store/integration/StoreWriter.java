package org.flockdata.store.integration;

import org.flockdata.helper.FlockServiceException;
import org.flockdata.store.bean.KvContentBean;
import org.flockdata.store.service.StoreManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.annotation.Retryable;

/**
 * Created by mike on 17/02/16.
 */
@Configuration
@IntegrationComponentScan
public class StoreWriter {

    @Autowired
    StoreManager storeManager;

    @Bean
    MessageChannel startKvWrite(){
        return new DirectChannel();
    }

    /**
     * Activated via an integration channel. This method goes through retry logic to handle
     * temporary failures. If the kvBean is not processed then the message is left on the queue
     * for retry
     * <p/>
     * For an add we write to the default store
     *
     * @param kvBean content
     * @throws FlockServiceException - problem with the underlying
     */
    @ServiceActivator(inputChannel = "doKvWrite", requiresReply = "false")
    @Retryable
    public void doKvWrite(KvContentBean kvBean) throws FlockServiceException {
        storeManager.doWrite(kvBean);
    }



}