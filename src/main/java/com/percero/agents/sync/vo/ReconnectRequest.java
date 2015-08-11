package com.percero.agents.sync.vo;

public class ReconnectRequest extends ConnectRequest
{
	private String existingClientId = "";
	public String getExistingClientId() {
		return existingClientId;
	}
	public void setExistingClientId(String existingClientId) {
		this.existingClientId = existingClientId;
	}

	private String[] existingClientIds = null;
	public String[] getExistingClientIds() {
		return existingClientIds;
	}
	public void setExistingClientIds(String[] existingClientIds) {
		this.existingClientIds = existingClientIds;
	}
}
