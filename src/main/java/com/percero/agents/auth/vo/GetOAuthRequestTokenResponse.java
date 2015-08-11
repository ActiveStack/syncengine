package com.percero.agents.auth.vo;

public class GetOAuthRequestTokenResponse extends AuthResponse {

	private String token;
	public String getToken() {
		return token;
	}
	public void setToken(String value) {
		this.token = value;
	}

	private String tokenSecret;
	public String getTokenSecret() {
		return tokenSecret;
	}
	public void setTokenSecret(String value) {
		this.tokenSecret = value;
	}
	
	private Integer expiresIn;
	public Integer getExpiresIn() {
		return expiresIn;
	}
	public void setExpiresIn(Integer value) {
		this.expiresIn = value;
	}
}
