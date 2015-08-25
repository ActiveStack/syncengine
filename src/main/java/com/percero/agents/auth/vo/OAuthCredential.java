package com.percero.agents.auth.vo;

/**
 * Should contain credential information to validate user with
 * an OAuth provider
 */
public class OAuthCredential {

    private String redirectUrl;
    private String code;

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
