package com.percero.agents.sync.vo;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public class LogoutRequest extends SyncRequest {

	private Boolean pleaseDestroyClient = false;
	public void setPleaseDestroyClient(Boolean value) {
		pleaseDestroyClient = value;
	}
	public Boolean getPleaseDestroyClient() {
		return pleaseDestroyClient;
	}
}
