package com.percero.agents.sync.vo;

import java.util.List;

public class FindByIdsRequest extends SyncRequest {

	private List<ClassIDPairs> theClassIdList;
	public List<ClassIDPairs> getTheClassIdList() {
		return theClassIdList;
	}
	public void setTheClassIdList(List<ClassIDPairs> theClassIdList) {
		this.theClassIdList = theClassIdList;
	}

}
