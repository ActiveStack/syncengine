package com.percero.agents.sync.vo;



public class PushDeletesReceivedRequest extends SyncRequest {

	private ClassIDPair[] classIdPairs;
	public ClassIDPair[] getClassIdPairs() {
		return classIdPairs;
	}
	public void setClassIdPairs(ClassIDPair[] classIdPairs) {
		this.classIdPairs = classIdPairs;
	}

}
