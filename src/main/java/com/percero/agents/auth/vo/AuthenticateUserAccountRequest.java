package com.percero.agents.auth.vo;

public class AuthenticateUserAccountRequest extends AuthRequest {

	private UserAccount userAccount;
	public UserAccount getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(UserAccount value) {
		userAccount = value;
	}
}
