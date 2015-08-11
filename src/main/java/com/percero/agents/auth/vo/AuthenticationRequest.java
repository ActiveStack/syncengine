package com.percero.agents.auth.vo;

import java.util.Map;

/**
 * This class represents a request from the client where they will specify the provider name
 * that they wish to authenticate against and a credential string that the provider will
 * use to authenticate the user.
 */
public class AuthenticationRequest extends AuthRequest {

    private String providerName;
    public String getProviderName() {
        return providerName;
    }
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    private String credential;
    public String getCredential() {
        return credential;
    }
    public void setCredential(String credential) {
        this.credential = credential;
    }

    private Map<String, String> context;
    public Map<String, String> getContext() {
        return context;
    }
    public void setContext(Map<String, String> context) {
        this.context = context;
    }
}
