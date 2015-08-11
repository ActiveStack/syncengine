package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;



public class CreateResponse extends SyncResponse {

	private Boolean result;
	public Boolean getResult() {
		return result;
	}
	public void setResult(Boolean result) {
		this.result = result;
	}
	
	private BaseDataObject theObject;
	public BaseDataObject getTheObject() {
		return theObject;
	}
	public void setTheObject(BaseDataObject value) {
		theObject = value;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",";
		objectJson += "\"theObject\":" + (getTheObject() == null ? "null" : getTheObject().toJson(objectMapper)) + ",";
		objectJson += "\"result\":" + (getResult() == null ? "null" : getResult());
		return objectJson;
	}
}
