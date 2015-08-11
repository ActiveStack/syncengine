package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;


public class PushUpdatesReceivedResponse extends SyncResponse {

	private Boolean result;
	public Boolean getResult() {
		return result;
	}
	public void setResult(Boolean result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":";
		objectJson += (getResult() == null ? "null" : getResult());
		return objectJson;
	}
}
