package com.percero.agents.sync.vo;

public class RunProcessRequest extends SyncRequest {

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
	
	private Boolean shardedProcess = false;
	public Boolean getShardedProcess() {
		return shardedProcess;
	}
	public void setShardedProcess(Boolean value) {
		shardedProcess = value;
	}
	
	private String serviceGroupId = null;
	public String getServiceGroupId() {
		return serviceGroupId;
	}
	public void setServiceGroupId(String value) {
		serviceGroupId = value;
	}
	
	private String processId = null;
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String value) {
		processId = value;
	}
}
