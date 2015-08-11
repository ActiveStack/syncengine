package com.percero.agents.sync.vo;


public class RemoveRequest extends SyncRequest {

	private ClassIDPair removePair;
	public ClassIDPair getRemovePair() {
		return removePair;
	}
	public void setRemovePair(ClassIDPair value) {
		this.removePair = value;
	}

}
