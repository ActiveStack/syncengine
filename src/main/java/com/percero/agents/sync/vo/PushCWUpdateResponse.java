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
		StringBuilder objectJson = new StringBuilder("{").append(super.retrieveJson(objectMapper)).append(",\"classIdPair\":");
		
		if (getClassIdPair() != null) {
			objectJson.append(getClassIdPair().toEmbeddedJson());
		}
		else {
			objectJson.append("null");
		}

		objectJson.append(",\"fieldName\":");
		if (getFieldName() == null)
			objectJson.append("null");
		else {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			try {
				objectJson.append(objectMapper.writeValueAsString(getFieldName()));
			} catch (JsonGenerationException e) {
				objectJson.append("null");
				e.printStackTrace();
			} catch (JsonMappingException e) {
				objectJson.append("null");
				e.printStackTrace();
			} catch (IOException e) {
				objectJson.append("null");
				e.printStackTrace();
			}
		}
		
		objectJson.append(",\"params\":");
		if (getParams() == null)
			objectJson.append("null");
		else {
			objectJson.append('[');
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			int paramsCounter = 0;
			for(String nextParam : getParams()) {
				try {
					if (paramsCounter > 0)
						objectJson.append(',');
					objectJson.append(objectMapper.writeValueAsString(nextParam));
					paramsCounter++;
				} catch (JsonGenerationException e) {
					objectJson.append("null");
					e.printStackTrace();
				} catch (JsonMappingException e) {
					objectJson.append("null");
					e.printStackTrace();
				} catch (IOException e) {
					objectJson.append("null");
					e.printStackTrace();
				}
			}
			objectJson.append(']');
		}
		
		objectJson.append(",\"value\":");
		if (value == null) {
			objectJson.append("null");
		}
		else {
			if (getValue() instanceof BaseDataObject) {
				objectJson.append(((BaseDataObject) getValue()).toEmbeddedJson());
			}
			else if (getValue() instanceof Collection) {
				Collection collection = (Collection) getValue();
				if (objectMapper == null)
					objectMapper = new ObjectMapper();
				
				objectJson.append('[');
				int collectionCounter = 0;
				for(Object nextCollectionObject : collection) {
					if (collectionCounter > 0)
						objectJson.append(',');
					if (nextCollectionObject instanceof BaseDataObject) {
						objectJson.append(((BaseDataObject) nextCollectionObject).toEmbeddedJson());
					}
					else {
						try {
							objectJson.append(objectMapper.writeValueAsString(nextCollectionObject));
						} catch (JsonGenerationException e) {
							objectJson.append("null");
							e.printStackTrace();
						} catch (JsonMappingException e) {
							objectJson.append("null");
							e.printStackTrace();
						} catch (IOException e) {
							objectJson.append("null");
							e.printStackTrace();
						}
					}
					
					collectionCounter++;
				}
				objectJson.append(']');
			}
			else {
				if (getValue() == null)
					objectJson.append("null");
				else {
					if (objectMapper == null)
						objectMapper = new ObjectMapper();
					try {
						objectJson.append(objectMapper.writeValueAsString(getValue()));
					} catch (JsonGenerationException e) {
						objectJson.append("null");
						e.printStackTrace();
					} catch (JsonMappingException e) {
						objectJson.append("null");
						e.printStackTrace();
					} catch (IOException e) {
						objectJson.append("null");
						e.printStackTrace();
					}
				}
			}
		}
		
		objectJson.append('}');
		
		return objectJson.toString();
	}

}
