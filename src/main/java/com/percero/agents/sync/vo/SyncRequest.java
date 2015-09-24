package com.percero.agents.sync.vo;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonTypeInfo;


@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
//@JsonIgnoreProperties(ignoreUnknown=true)
public class SyncRequest {
	
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

	private Boolean sendAck;
	
	public Boolean getSendAck() {
		return sendAck;
	}
	public void setSendAck(Boolean value) {
		this.sendAck = value;
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
	
	private String responseChannel;
	
	public String getResponseChannel() {
		return responseChannel;
	}
	public void setResponseChannel(String responseChannel) {
		this.responseChannel = responseChannel;
	}

	private String deviceId = "";
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	private String regAppKey;
	public void setRegAppKey(String value) {
		regAppKey = value;
	}
	public String getRegAppKey() {
		return regAppKey;
	}

	private String svcOauthKey;
	public String getSvcOauthKey() {
		return svcOauthKey;
	}
	public void setSvcOauthKey(String value) {
		svcOauthKey = value;
	}

	private String messageId;
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
}
