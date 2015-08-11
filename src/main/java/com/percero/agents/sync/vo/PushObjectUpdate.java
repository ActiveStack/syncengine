package com.percero.agents.sync.vo;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.percero.framework.vo.IPerceroObject;



public class PushObjectUpdate extends SyncResponse {

	private BaseDataObject result;
	public BaseDataObject getResult() {
		return result;
	}
	public void setResult(BaseDataObject result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",";
		objectJson += "\"result\":" + (getResult() == null ? "null" : getResult().toJson(objectMapper));

		objectJson += ",\"fieldNames\":[";
		if (getFieldNames() != null && getFieldNames().length > 0) {
			if (objectMapper == null)
				objectMapper = new ObjectMapper();
			for(int i=0; i < getFieldNames().length; i++) {
				try {
					objectJson += objectMapper.writeValueAsString(getFieldNames()[i]);
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
		objectJson += "]";

		return objectJson;
	}
	
	private String[] fieldNames;
	public String[] getFieldNames() {
		return fieldNames;
	}
	public void setFieldNames(String[] value) {
		fieldNames = value;
	}

	/**
	public String toJson(ObjectMapper objectMapper) {
		String objectJson = "{" + super.retrieveJson(objectMapper) + ",\"classIdPair\":";
		
		if (getClassIdPair() != null) {
			objectJson += getClassIdPair().toEmbeddedJson();
		}
		else {
			objectJson += "null";
		}

		objectJson += ",\"fieldNames\":[";
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
		objectJson += "]";
		
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
	*/
}
