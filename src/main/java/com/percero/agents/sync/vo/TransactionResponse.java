package com.percero.agents.sync.vo;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;


public class TransactionResponse extends SyncResponse {

	private String transactionId;
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String value) {
		transactionId = value;
	}
	
	private Boolean result;
	public Boolean getResult() {
		return result;
	}
	public void setResult(Boolean result) {
		this.result = result;
	}

	private List<ClassIDPair> objectsSaved;
	public List<ClassIDPair> getObjectsSaved() {
		return objectsSaved;
	}
	public void setObjectsSaved(List<ClassIDPair> values) {
		this.objectsSaved = values;
	}

	private List<ClassIDPair> objectsRemoved;
	public List<ClassIDPair> getObjectsRemoved() {
		return objectsRemoved;
	}
	public void setObjectsRemoved(List<ClassIDPair> values) {
		this.objectsRemoved = values;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"transactionId\":";
		objectJson += (getTransactionId() == null ? "null" : getTransactionId()) + ",";
		objectJson += "\"result\":" + (getResult() == null ? "null" : getResult()) + ",";
		
		int counter = 0;
		objectJson += "\"objectsSaved\":[";
		for(ClassIDPair nextPair : getObjectsSaved()) {
			if (counter > 0)
				objectJson += ",";
			objectJson += nextPair.toEmbeddedJson();
			counter++;
		}
		objectJson += "],";
		
		counter = 0;
		objectJson += "\"objectsRemoved\":[";
		for(ClassIDPair nextPair : getObjectsRemoved()) {
			if (counter > 0)
				objectJson += ",";
			objectJson += nextPair.toEmbeddedJson();
			counter++;
		}
		objectJson += "]";
		return objectJson;
	}
}
