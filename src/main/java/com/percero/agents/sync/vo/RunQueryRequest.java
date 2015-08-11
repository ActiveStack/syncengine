package com.percero.agents.sync.vo;

public class RunQueryRequest extends SyncRequest {

	private String theClassName;
	public String getTheClassName() {
		return theClassName;
	}
	public void setTheClassName(String className) {
		this.theClassName = className;
	}

	private String queryName;
	public String getQueryName() {
		return queryName;
	}
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	private Object[] queryArguments;
	public Object[] getQueryArguments() {
		return queryArguments;
	}
	public void setQueryArguments(Object[] queryArguments) {
		this.queryArguments = queryArguments;
	}
}
