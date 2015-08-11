package com.percero.agents.sync.vo;


public class PutRequest extends SyncRequest {

	private BaseDataObject theObject;
	public BaseDataObject getTheObject() {
		return theObject;
	}
	public void setTheObject(BaseDataObject theObject) {
		this.theObject = theObject;
	}

	private Long putTimestamp;
	public Long getPutTimestamp() {
		return putTimestamp;
	}
	public void setPutTimestamp(Long value) {
		putTimestamp = value;
	}
	
	private String transId;
	public String getTransId() {
		return transId;
	}
	public void setTransId(String value) {
		transId = value;
	}
}
