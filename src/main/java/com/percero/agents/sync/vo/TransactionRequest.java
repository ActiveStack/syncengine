package com.percero.agents.sync.vo;

import java.util.List;


public class TransactionRequest extends SyncRequest {

	private String transactionId;
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String value) {
		transactionId = value;
	}
	
	private Long transTimestamp;
	public Long getTransTimestamp() {
		return transTimestamp;
	}
	public void setTransTimestamp(Long value) {
		transTimestamp = value;
	}
	
	private List<BaseDataObject> objectsToSave;
	public List<BaseDataObject> getObjectsToSave() {
		return objectsToSave;
	}
	public void setObjectsToSave(List<BaseDataObject> values) {
		this.objectsToSave = values;
	}
	
	private List<BaseDataObject> objectsToRemove;
	public List<BaseDataObject> getObjectsToRemove() {
		return objectsToRemove;
	}
	public void setObjectsToRemove(List<BaseDataObject> values) {
		this.objectsToRemove = values;
	}

}
