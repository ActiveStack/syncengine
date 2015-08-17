package com.percero.agents.auth.vo;

/**
 * Created by jonnysamps on 8/17/15.
 */
public class AuthenticationResponse extends AuthResponse {

    private UserToken result;
    public UserToken getResult() {
        return result;
    }
    public void setResult(UserToken result) {
        this.result = result;
    }

}
