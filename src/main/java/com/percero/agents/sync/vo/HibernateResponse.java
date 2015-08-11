package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;

public class HibernateResponse extends SyncResponse {

	private Boolean result;
	public void setResult(Boolean value) {
		result = value;
	}
	
	public Boolean getResult() {
		return result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":";
		objectJson += (getResult() == null ? "null" : getResult());
		return objectJson;
	}
}
