package com.percero.agents.sync.vo;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.jackson.map.ObjectMapper;



public class PushUpdateResponse extends SyncResponse {

	private List<BaseDataObject> objectList;
	public List<BaseDataObject> getObjectList() {
		return objectList;
	}
	public void setObjectList(List<BaseDataObject> value) {
		objectList = value;
	}
	
	private SortedSet<String> updatedFields = new TreeSet<String>();
	public SortedSet<String> getUpdatedFields() {
		return updatedFields;
	}
	public void setUpdatedFields(SortedSet<String> value) {
		updatedFields = value;
	}
	
	public void addUpdatedField(String fieldName) {
		if (updatedFields == null) {
			updatedFields = new TreeSet<String>();
		}
		
		updatedFields.add(fieldName);
	}
	
	public String toJson(ObjectMapper objectMapper) {
		if (objectJson == null) {
			String objectListJson = "";
			int objectCounter = 0;
			for(BaseDataObject nextObject : getObjectList()) {
				if (objectCounter > 0)
					objectListJson += ",";
				objectListJson += nextObject.toJson();
				objectCounter++;
			}
			return toJson(objectListJson, objectMapper);
		}
		else {
			return toJson(objectJson, objectMapper);
		}
	}
	
	protected String objectJson = null;
	public void setObjectJson(String value) {
		objectJson = value;
	}
	protected String getObjectJson() {
		return objectJson;
	}
	
	public String toJson(String objectListJson, ObjectMapper objectMapper) {
		String objectJson = "{" + super.retrieveJson(objectMapper) + ",\"objectList\":[";
		objectJson += objectListJson + "],\"updatedFields\":[";

		int partialsCounter = 0;
		if (getUpdatedFields() != null) {
			Iterator<String> itrUpdatedFields = getUpdatedFields().iterator();
			while (itrUpdatedFields.hasNext()) {
				String nextField = itrUpdatedFields.next();
				if (partialsCounter > 0)
					objectJson += ",";
				objectJson += "\"" + nextField + "\"";	// Because this is a field name we shouldn't need any special string transforms.
				partialsCounter++;
			}
		}
		else {
			objectJson += "null";
		}
		
		objectJson += "]";

		objectJson += "}";
		return objectJson;
	}

}
