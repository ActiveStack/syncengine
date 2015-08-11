package com.percero.agents.sync.vo;

public class GetAccessorRequest extends SyncRequest {

	private Boolean returnObject;
	public Boolean getReturnObject() {
		return returnObject;
	}
	public void setReturnObject(Boolean returnObject) {
		this.returnObject = returnObject;
	}

	private String theClassName;
	public String getTheClassName() {
		return theClassName;
	}
	public void setTheClassName(String theClassName) {
		this.theClassName = theClassName;
	}

	private String theClassId;
	public String getTheClassId() {
		return theClassId;
	}
	public void setTheClassId(String theClassId) {
		this.theClassId = theClassId;
	}

}
