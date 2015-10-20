package com.percero.agents.auth.vo;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public class AuthResponse {

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
	
	private String correspondingMessageId;
	public String getCorrespondingMessageId() {
		return correspondingMessageId;
	}
	public void setCorrespondingMessageId(String correspondingMessageId) {
		this.correspondingMessageId = correspondingMessageId;
	}

	private String message = "OK";
	public String getMessage(){
		return this.message;
	}
	public void setMessage(String message){
		this.message = message;
	}

	private int statusCode = 200;
	public int getStatusCode(){
		return this.statusCode;
	}
	public void setStatusCode(int statusCode){
		this.statusCode = statusCode;
	}
}
