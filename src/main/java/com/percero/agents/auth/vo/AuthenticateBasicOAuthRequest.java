package com.percero.agents.auth.vo;

public class AuthenticateBasicOAuthRequest extends AuthRequest {

	private String userName;
	public String getUserName() {
		return userName;
	}
	public void setUserName(String value) {
		userName = value;
	}

	private String password;
	public String getPassword() {
		return password;
	}
	public void setPassword(String value) {
		password = value;
	}
	
	private String scopes;
	public String getScopes() {
		return scopes;
	}
	public void setScopes(String value) {
		scopes = value;
	}

	private String appUrl;
	public String getAppUrl() {
		return appUrl;
	}
	public void setAppUrl(String value) {
		appUrl = value;
	}
	
	private String requestToken;
	public String getRequestToken() {
		return requestToken;
	}
	public void setRequestToken(String value) {
		requestToken = value;
	}
	
	private String requestSecret;
	public String getRequestSecret() {
		return requestSecret;
	}
	public void setRequestSecret(String value) {
		requestSecret = value;
	}
	
}
