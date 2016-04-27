package com.percero.agents.auth.services;

import com.percero.agents.auth.vo.AuthProviderResponse;

/**
 * Defines the interface for providing additional authentication behavior into the ActiveStack
 */
public interface IAuthProvider {
    /**
     * Returns the string that uniquely identifies this provider
     * @return String
     */
    String getID();

    /**
     * Authenticates a token and returns a ServiceUser that corresponds to the credentials provided
     * @param credential - A String to be interpreted by the provider as an authentication credential
     * @return ServiceUser
     */
    AuthProviderResponse authenticate(String credential);

    /**
     * If supported but the AuthProvider, registers the user.
     * @param credential - A String to be interpreted by the provider as an authentication credential
     * @return
     */
    AuthProviderResponse register(String credential);
}
