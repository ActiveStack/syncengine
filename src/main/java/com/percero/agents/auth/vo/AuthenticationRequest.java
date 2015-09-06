package com.percero.agents.auth.vo;

/**
 * This class represents a request from the client where they will specify the provider name
 * that they wish to authenticate against and a credential string that the provider will
 * use to authenticate the user.
 */
public class AuthenticationRequest extends AuthRequest {

    private String credential;
    public String getCredential() {
        return credential;
    }
    public void setCredential(String credential) {
        this.credential = credential;
    }

}
