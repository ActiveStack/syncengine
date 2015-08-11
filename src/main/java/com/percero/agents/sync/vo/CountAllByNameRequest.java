package com.percero.agents.sync.vo;

import java.util.Collection;

public class CountAllByNameRequest extends SyncRequest {

	private Collection<String> classNames;
	public Collection<String> getClassNames() {
		return classNames;
	}
	public void setClassNames(Collection<String> classNames) {
		this.classNames = classNames;
	}
}
