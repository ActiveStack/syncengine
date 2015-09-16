package com.percero.client;

import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Main interface for communicating to the backend through AMQP
 * Created by jonnysamps on 9/13/15.
 */
@Component
public class AStackRPCService {

    @Autowired
    AmqpTemplate amqpTemplate;

    public AuthenticationResponse authenticate(AuthenticationRequest request){
        return (AuthenticationResponse) amqpTemplate.convertSendAndReceive("authenticate", request);
    }

}
