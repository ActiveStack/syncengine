package com.percero.agents.auth.vo;

public class AuthenticateOAuthAccessTokenResponse extends AuthResponse {

	public static final String ID = "142";
	
	private UserToken result;
	public UserToken getResult() {
		return result;
	}
	public void setResult(UserToken result) {
		this.result = result;
	}

	private String accessToken;
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String value) {
		this.accessToken = value;
	}

	private String refreshToken;
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String value) {
		this.refreshToken = value;
	}
}
