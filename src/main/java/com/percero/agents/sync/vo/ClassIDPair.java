package com.percero.agents.sync.vo;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import com.percero.framework.vo.IPerceroObject;

public class ClassIDPair implements Externalizable, IClassIDPair {

	private static Logger logger = Logger.getLogger(ClassIDPair.class);
	
	private String ID = "";
	private String className = "";
	private Map<String, Object> properties;

	public ClassIDPair() {
		
	}

	public ClassIDPair(String anId, String aClassName) {
		setID(anId);
		setClassName(aClassName);
	}
	
	@JsonProperty(value="ID")
	public String getID() {
		return ID;
	}
	@JsonProperty(value="ID")
	public void setID(String value) {
		ID = value;
	}
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String value) {
		className = value;
	}
	
	public Map<String, Object> getProperties()
	{
		return properties;
	}
	public void setProperties(Map<String, Object> value) {
		properties = value;
	}
	public void addProperty(String name, Object value) {
		if (properties == null)
			setProperties(new HashMap<String, Object>());
		
		properties.put(name, value);
	}
	
	public void readExternal(ObjectInput input) {
		try {
			setID(input.readUTF());
			setClassName(input.readUTF());
		} catch(Exception e) {
			logger.error("Error deserializing ClassIDPair", e);
		}
	}
	
	public void writeExternal(ObjectOutput output) {
		try {
			output.writeUTF(getID());
			output.writeUTF(getClassName());
		} catch(Exception e) {
			logger.error("Error serializing ClassIDPair", e);
		}
	}
	
	public String toJson() {
		return toJson(true, null);
	}
	
	public String toJson(ObjectMapper objectMapper) {
		return toJson(true, objectMapper);
	}
	
	public String toJson(Boolean encloseString, ObjectMapper objectMapper) {
		String objectJson = retrieveJson(objectMapper);
		if (encloseString)
			objectJson = "{" + objectJson + "}";
		return objectJson;
	}
	
	public String retrieveJson(ObjectMapper objectMapper) {
		return retrieveBaseJson();
	}
	
	public String retrieveBaseJson() {
		String objectJson = "\"className\":\"" + getClassName() + "\"," + 
				"\"ID\":\"" + getID() + "\"";
		
		return objectJson;
	}

	public String toEmbeddedJson() {
		return toEmbeddedJson(true);
	}
	
	public String toEmbeddedJson(Boolean encloseString) {
		String objectJson = retrieveEmbeddedJson();
		if (encloseString)
			objectJson = "{" + objectJson + "}";
		return objectJson;
	}
	
	public String retrieveEmbeddedJson() {
		String objectJson = "\"className\":\"" + getClassName() + "\"," + 
				"\"ID\":\"" + getID() + "\"";
		
		return objectJson;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassIDPair) {
			ClassIDPair compareTo = (ClassIDPair) obj;
			if (compareTo.getClassName() == null && getClassName() == null && compareTo.getID() == null && getID() == null) {
				return true;
			}
			else if (compareTo.getClassName() == null || getClassName() == null) {
				return false;
			}
			else if (compareTo.getID() == null || getID() == null) {
				return false;
			}
			if (compareTo.getClassName().equals(getClassName()) && compareTo.getID().equals(getID())) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean comparePerceroObject(IPerceroObject compareTo) {
		if (compareTo != null) {
			if (compareTo.getClass().getCanonicalName() == null && getClassName() == null && compareTo.getID() == null && getID() == null) {
				return true;
			}
			else if (compareTo.getClass().getCanonicalName() == null || getClassName() == null) {
				return false;
			}
			else if (compareTo.getID() == null || getID() == null) {
				return false;
			}
			if (compareTo.getClass().getCanonicalName().equals(getClassName()) && compareTo.getID().equals(getID())) {
				return true;
			}
		}
		
		return false;
	}

}
