package com.percero.agents.sync.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class PutFieldsRequest extends PutRequest {

	private ClassIDPair classIdPair;

	public ClassIDPair getClassIdPair() {
		return classIdPair;
	}

	public void setClassIdPair(ClassIDPair classIdPair) {
		this.classIdPair = classIdPair;
	}
	
	private Map<String, Object> fields;
	
	public Map<String, Object> getFields() {
		return fields;
	}
	
	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}
	
	public Map<String, Object> deserializeFields() {
		Map<String, Object> result = new HashMap<String, Object>();
		
		if (getFields() != null) {
			Iterator<Map.Entry<String, Object>> itrFields = getFields().entrySet().iterator();
			while (itrFields.hasNext()) {
				Map.Entry<String, Object> nextEntry = itrFields.next();
				String key = nextEntry.getKey();
				try {
					Object value = nextEntry.getValue();
//			for(String key : getFields().keySet()) {
//				try {
//					Object value = getFields().get(key);
					if (value instanceof Map) {
						Map mapValue = (Map) value;
						if (mapValue.keySet().contains("ID") && mapValue.keySet().contains("className")) {
							// This is a class id pair object.
							ClassIDPair classIdPair = new ClassIDPair((String)mapValue.get("ID"), (String)mapValue.get("className"));
							result.put(key, classIdPair);
						}
						else {
							System.out.println("Unknown field in putFieldsRequest: " + key);
						}
					}
					else if (value instanceof List) {
						List<Object> nextResultList = new ArrayList<Object>();
						result.put(key, nextResultList);
						for(Object nextListValue : (List)value) {
							if (nextListValue instanceof Map) {
								Map mapValue = (Map) nextListValue;
								if (mapValue.keySet().contains("ID") && mapValue.keySet().contains("className")) {
									// This is a class id pair object.
									ClassIDPair classIdPair = new ClassIDPair((String)mapValue.get("ID"), (String)mapValue.get("className"));
									nextResultList.add(classIdPair);
								}
								else {
									System.out.println("Unknown field in putFieldsRequest: " + key);
								}
							}
						}
					}
					else {
						result.put(key, value);
					}
				} catch(Exception e) {
					System.out.println("Error parsing field in putFieldsRequest: " + key);
				}
			}
		}
		
		return result;
	}
	
}
