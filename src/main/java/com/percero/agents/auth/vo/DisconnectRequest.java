package com.percero.agents.auth.vo;

import java.util.Set;

public class DisconnectRequest extends AuthRequest {

	private String existingClientId;
	public String getExistingClientId() {
		return existingClientId;
	}
	public void setExistingClientId(String existingClientId) {
		this.existingClientId = existingClientId;
	}
	
	private Set<String> existingClientIds;
	public Set<String> getExistingClientIds() {
		return existingClientIds;
	}
	public void setExistingClientIds(Set<String> existingClientIds) {
		this.existingClientIds = existingClientIds;
	}
}
