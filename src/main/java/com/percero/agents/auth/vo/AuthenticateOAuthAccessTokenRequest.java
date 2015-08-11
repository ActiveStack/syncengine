package com.percero.agents.auth.vo;

public class AuthenticateOAuthAccessTokenRequest extends AuthRequest {

	public static final String ID = "42";

	private String accessToken;
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String value) {
		accessToken = value;
	}

	private String refreshToken;
	public String getRefreshToken() {
		return refreshToken;
	}
	public void setRefreshToken(String value) {
		refreshToken = value;
	}

	private String redirectUri;
	public String getRedirectUri() {
		return redirectUri;
	}
	public void setRedirectUri(String value) {
		redirectUri = value;
	}
}
