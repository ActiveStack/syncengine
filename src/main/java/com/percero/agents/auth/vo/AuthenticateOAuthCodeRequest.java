package com.percero.agents.auth.vo;

public class AuthenticateOAuthCodeRequest extends AuthRequest {

	public static final String ID = "41";
	
	private String code;
	public String getCode() {
		return code;
	}
	public void setCode(String value) {
		code = value;
	}

	private String redirectUri;
	public String getRedirectUri() {
		return redirectUri;
	}
	public void setRedirectUri(String value) {
		redirectUri = value;
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
