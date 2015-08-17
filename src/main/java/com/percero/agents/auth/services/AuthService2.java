package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.ServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class handles AuthenticationRequest type authentication
 */
@Component
public class AuthService2 {

    @Autowired
    private AuthProviderRegistry authProviderRegistry;

    public ServiceUser authenticate(String providerID, String credential) throws IllegalArgumentException{
        if(!authProviderRegistry.hasProvider(providerID))
            throw new IllegalArgumentException(providerID+" auth provider not found");

        ServiceUser result = null;

        IAuthProvider provider = authProviderRegistry.getProvider(providerID);
        result = provider.authenticate(credential);

        // TODO: save a user to DB?

        return result;
    }
}
