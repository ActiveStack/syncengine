package com.percero.test.utils;

import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.client.AStackRPCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jonnysamps on 9/13/15.
 */
@Component
public class AuthUtil {

    @Autowired
    AStackRPCService rpcService;

    public AuthenticationResponse authenticateAnonymously(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setAuthProvider("anonymous");
        AuthenticationResponse response = (AuthenticationResponse) rpcService.authenticate(request);
        return response;
    }
}
