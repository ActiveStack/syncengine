package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;

public class ConnectResponse extends SyncResponse {

	/**
	 * Used by the clients to sync their clock's time to the server's time.
	 */
	private Long currentTimestamp;
	public Long getCurrentTimestamp() {
		return currentTimestamp;
	}
	public void setCurrentTimestamp(Long value) {
		currentTimestamp = value;
	}
	
	private String dataID;
	public String getDataID() {
		return dataID;
	}
	public void setDataID(String value) {
		dataID = value;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",";
		objectJson += "\"currentTimestamp\":" + (getCurrentTimestamp() == null ? "null" : getCurrentTimestamp()) + ",";
		objectJson += "\"dataID\":" + (getDataID() == null ? "null" : "\"" + getDataID() + "\"");
		return objectJson;
	}
}
