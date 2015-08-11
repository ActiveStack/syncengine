package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;



public class FindByIdResponse extends SyncResponse {

	private BaseDataObject result;
	public BaseDataObject getResult() {
		return result;
	}
	public void setResult(BaseDataObject result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",";
		objectJson += "\"result\":" + (getResult() == null ? "null" : getResult().toJson(objectMapper));
		return objectJson;
	}
}
