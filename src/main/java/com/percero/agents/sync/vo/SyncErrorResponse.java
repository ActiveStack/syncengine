package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;


public class SyncErrorResponse extends SyncResponse {

	private String errorName;
	public String getErrorName() {
		return errorName;
	}
	public void setErrorName(String value) {
		this.errorName = value;
	}
	
	private String errorDesc;
	public String getErrorDesc() {
		return errorDesc;
	}
	public void setErrorDesc(String value) {
		this.errorDesc = value;
	}
	
	private Integer errorCode;
	public Integer getErrorCode() {
		return this.errorCode;
	}
	public void setErrorCode(Integer value) {
		this.errorCode = value;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"errorName\":";
		objectJson += (getErrorName() == null ? "null" : "\"" + getErrorName() + "\"") + ",";
		objectJson += "\"errorDesc\":" + (getErrorDesc() == null ? "null" : "\"" + getErrorDesc() + "\"") + ",";
		objectJson += "\"errorCode\":" + (getErrorCode() == null ? "null" : "\"" + getErrorCode() + "\"");
		return objectJson;
	}
}
