package com.percero.agents.sync.vo;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.log4j.Logger;

import com.percero.agents.sync.manager.DataExternalizer;

public class ServerRequest implements Externalizable {

	protected static Logger logger = Logger.getLogger(ServerRequest.class);
	
	public Boolean validateRequest() {
		return true;
	}
	
	private Object requestData = null;
	private String requestType = "";

	public Object getRequestData() {
		return requestData;
	}

	public void setRequestData(Object requestData) {
		this.requestData = requestData;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public void readExternal(ObjectInput input) {
		DataExternalizer.readExternal(input, this);
	}
	
	public void writeExternal(ObjectOutput output) {
		DataExternalizer.writeExternal(output, this);
	}
}
