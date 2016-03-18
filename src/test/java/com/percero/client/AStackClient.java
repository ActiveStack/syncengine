package com.percero.client;

import java.util.UUID;

import com.percero.agents.auth.vo.AuthenticationRequest;
import com.percero.agents.auth.vo.AuthenticationResponse;
import com.percero.agents.auth.vo.UserToken;

/**
 * Intended to maintain session with the server and listen for
 * push messages.
 */
public class AStackClient {

    /**
     * Some service deps
     */
    private AStackRPCService rpcService;
    private String clientId = UUID.randomUUID().toString();
    public AStackClient(AStackRPCService rpcService){
        this.rpcService             = rpcService;
    }

    /**
     * Some internal state
     */
    private UserToken userToken;

    public boolean authenticateAnonymously(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setAuthProvider("anonymous");
        request.setClientId(clientId);

        AuthenticationResponse response = rpcService.authenticate(request);
        userToken = response.getResult();
        boolean result = (userToken != null);

        return result;
    }
}
