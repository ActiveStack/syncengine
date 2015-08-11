package com.percero.agents.auth.vo;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public class AuthRequest {

	private String userId;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	private String token;

	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}

	private String clientType;
	
	public String getClientType() {
		return clientType;
	}
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	private String clientId = "";
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	private String deviceId = "";
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	private String messageId;
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	private String regAppKey;
	public void setRegAppKey(String value) {
		regAppKey = value;
	}
	public String getRegAppKey() {
		return regAppKey;
	}

	private AuthProvider authProvider;
	public AuthProvider getAuthProvider() {
		return authProvider;
	}
	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}
}
