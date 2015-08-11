package com.percero.agents.sync.vo;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;

public class ClassIDPairs implements Externalizable {

	private static Logger logger = Logger.getLogger(ClassIDPairs.class);
	
	private List<String> ids = new ArrayList<String>();
	private String className = "";

	public ClassIDPairs() {
		
	}

	@JsonProperty(value="ids")
	public List<String> getIds() {
		return ids;
	}
	@JsonProperty(value="ids")
	public void setIds(List<String> value) {
		ids = value;
	}
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String value) {
		className = value;
	}
	
	public void readExternal(ObjectInput input) {
		try {
			setClassName(input.readUTF());
			String[] idsArray = (String[]) input.readObject();
			ids = new ArrayList<String>();
			for(String nextId : idsArray) {
				ids.add(nextId);
			}
		} catch(Exception e) {
			logger.error("Error deserializing ClassIDPair", e);
		}
	}
	
	public void writeExternal(ObjectOutput output) {
		try {
			output.writeUTF(getClassName());
			output.writeObject(ids.toArray());
		} catch(Exception e) {
			logger.error("Error serializing ClassIDPair", e);
		}
	}
}
