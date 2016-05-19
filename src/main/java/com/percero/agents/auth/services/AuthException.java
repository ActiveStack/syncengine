package com.percero.agents.auth.services;

public class AuthException extends Exception {
	
	public static final String DATA_ERROR = "dataError";
	public static final String DUPLICATE_USER_NAME = "duplicateUserName";
	public static final String INVALID_USER_IDENTIFIER = "invalidUserIdentifier";
	public static final String INVALID_USER_PASSWORD = "invalidUserPassword";
	public static final String INVALID_DATA = "invalidData";

	public AuthException() {
		// TODO Auto-generated constructor stub
	}
	
	private String detail = "";
	public void setDetail(String detail) {
		this.detail = detail;
	}
	public String getDetail() {
		return detail;
	}

	public AuthException(String message) {
		super(message);
	}

	public AuthException(String message, String detail) {
		super(message);
		this.detail = detail;
	}
	
	public AuthException(Throwable cause) {
		super(cause);
	}

	public AuthException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthException(String message, String detail, Throwable cause) {
		super(message, cause);
		this.detail = detail;
	}
	
	public AuthException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
