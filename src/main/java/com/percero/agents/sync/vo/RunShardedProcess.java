package com.percero.agents.sync.vo;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public class RunShardedProcess {

	public RunShardedProcess() {
		// TODO Auto-generated constructor stub
	}

	private String originatingClientId;
	private String clientId;
	private String serviceGroupId;
	private String processId;
	private Object arguments;

	public String getOriginatingClientId() {
		return originatingClientId;
	}
	public void setOriginatingClientId(String originatingClientId) {
		this.originatingClientId = originatingClientId;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getServiceGroupId() {
		return serviceGroupId;
	}
	public void setServiceGroupId(String serviceGroupId) {
		this.serviceGroupId = serviceGroupId;
	}
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public Object getArguments() {
		return arguments;
	}
	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}
}
