package com.percero.agents.sync.vo;


public class CreateRequest extends SyncRequest {

	private BaseDataObject theObject;
	public BaseDataObject getTheObject() {
		return theObject;
	}
	public void setTheObject(BaseDataObject theObject) {
		this.theObject = theObject;
	}

}
