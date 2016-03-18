package com.percero.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.percero.agents.auth.services.AuthService2;
import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;

/**
 * Main interface for communicating to the backend through AMQP
 * Created by jonnysamps on 9/13/15.
 */
@Component
public class AStackRPCService {

    @Autowired
    AuthService2 authService2;

    public AuthenticationResponse authenticate(AuthenticationRequest request){
        AuthenticationResponse response = (AuthenticationResponse) authService2.authenticate(request);
    	return response;
    }

}
