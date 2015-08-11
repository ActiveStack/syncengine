package com.percero.agents.sync.vo;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.log4j.Logger;

public class ServerResponse implements Externalizable {
	private static final long serialVersionUID = -8309093913424163758L;
	private static Logger logger = Logger.getLogger(ServerResponse.class);
	

	
	private Boolean isSuccessful = false;
	private Object resultObject = null;
	
	public Boolean getIsSuccessful() {
		return isSuccessful;
	}
	public void setIsSuccessful(Boolean value) {
		isSuccessful = value;
	}

	public Object getResultObject() {
		return resultObject;
	}
	public void setResultObject(Object value) {
		resultObject = value;
	}

	public void readExternal(ObjectInput input) {
		try {
			setIsSuccessful(input.readBoolean());
			setResultObject(input.readObject());
		} catch(Exception e) {
			logger.error("Error deserializing ServerResponse", e);
		}
	}
	
	public void writeExternal(ObjectOutput output) {
		try {
			output.writeBoolean(getIsSuccessful());
			output.writeObject(getResultObject());
		} catch(Exception e) {
			logger.error("Error serializing ServerResponse", e);
		}
	}
}
