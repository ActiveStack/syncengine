package com.percero.agents.sync.vo;

import java.io.IOException;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;



public class PushCWUpdateResponse extends SyncResponse {

	private ClassIDPair classIdPair;
	public ClassIDPair getClassIdPair() {
		return classIdPair;
	}
	public void setClassIdPair(ClassIDPair value) {
		classIdPair = value;
	}
	
	private String fieldName;
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String value) {
		fieldName = value;
	}
	
	private String[] params;
	public String[] getParams() {
		return params;
	}
	public void setParams(String[] value) {
		params = value;
	}
	
	private Object value;
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
	@SuppressWarnings("rawtypes")
	public String toJson(ObjectMapper objectMapper) {
		String objectJson = "{" + super.retrieveJson(objectMapper) + ",\"classIdPair\":";
		
		if (getClassIdPair() != null) {
			objectJson += getClassIdPair().toEmbeddedJson();
		}
		else {
			objectJson += "null";
		}

		objectJson += ",\"fieldName\":";
		if (getFieldName() == null)
			objectJson += "null";
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson += objectMapper.writeValueAsString(getFieldName());
			} catch (JsonGenerationException e) {
				objectJson += "null";
				e.printStackTrace();
			} catch (JsonMappingException e) {
				objectJson += "null";
				e.printStackTrace();
			} catch (IOException e) {
				objectJson += "null";
				e.printStackTrace();
			}
		}
		
		objectJson += ",\"params\":";
		if (getParams() == null)
			objectJson += "null";
		else {
			objectJson += "[";
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			int paramsCounter = 0;
			for(String nextParam : getParams()) {
				try {
					if (paramsCounter > 0)
						objectJson += ",";
					objectJson += objectMapper.writeValueAsString(nextParam);
					paramsCounter++;
				} catch (JsonGenerationException e) {
					objectJson += "null";
					e.printStackTrace();
				} catch (JsonMappingException e) {
					objectJson += "null";
					e.printStackTrace();
				} catch (IOException e) {
					objectJson += "null";
					e.printStackTrace();
				}
			}
			objectJson += "]";
		}
		
		objectJson += ",\"value\":";
		if (value == null) {
			objectJson += "null";
		}
		else {
			if (getValue() instanceof BaseDataObject) {
				objectJson += ((BaseDataObject) getValue()).toEmbeddedJson();
			}
			else if (getValue() instanceof Collection) {
				Collection collection = (Collection) getValue();
				if (objectMapper == null)
					objectMapper = new ObjectMapper();
				
				objectJson += "[";
				int collectionCounter = 0;
				for(Object nextCollectionObject : collection) {
					if (collectionCounter > 0)
						objectJson += ",";
					if (nextCollectionObject instanceof BaseDataObject) {
						objectJson += ((BaseDataObject) nextCollectionObject).toEmbeddedJson();
					}
					else {
						try {
							objectJson += objectMapper.writeValueAsString(nextCollectionObject);
						} catch (JsonGenerationException e) {
							objectJson += "null";
							e.printStackTrace();
						} catch (JsonMappingException e) {
							objectJson += "null";
							e.printStackTrace();
						} catch (IOException e) {
							objectJson += "null";
							e.printStackTrace();
						}
					}
					
					collectionCounter++;
				}
				objectJson += "]";
			}
			else {
				if (getValue() == null)
					objectJson += "null";
				else {
					if (objectMapper == null)
						objectMapper = new ObjectMapper();
					try {
						objectJson += objectMapper.writeValueAsString(getValue());
					} catch (JsonGenerationException e) {
						objectJson += "null";
						e.printStackTrace();
					} catch (JsonMappingException e) {
						objectJson += "null";
						e.printStackTrace();
					} catch (IOException e) {
						objectJson += "null";
						e.printStackTrace();
					}
				}
			}
		}
		
		objectJson += "}";
		
		return objectJson;
	}

}
