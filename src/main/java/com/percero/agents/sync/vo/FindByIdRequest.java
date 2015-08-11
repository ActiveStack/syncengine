package com.percero.agents.sync.vo;

public class FindByIdRequest extends SyncRequest {

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
