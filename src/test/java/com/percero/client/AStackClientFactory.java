package com.percero.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jonnysamps on 9/14/15.
 */
@Component
public class AStackClientFactory {

    @Autowired
    AStackRPCService rpcService;

    public AStackClient getClient(){
        return new AStackClient(rpcService);
    }
}
