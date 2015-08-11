package com.percero.agents.auth.vo;

public class AuthenticateUserAccountResponse extends AuthResponse {

	private UserToken result;
	public UserToken getResult() {
		return result;
	}
	public void setResult(UserToken result) {
		this.result = result;
	}
}
