package com.percero.agents.sync.vo;

import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;


public class CountAllByNameResponse extends SyncResponse {

	private Map<String, Integer> result;
	public Map<String, Integer> getResult() {
		return result;
	}
	public void setResult(Map<String, Integer> result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":[";
		int counter = 0;
		for(String nextKey : getResult().keySet()) {
			if (counter > 0)
				objectJson += ",";
			objectJson += "{\"" + nextKey + "\": " + getResult().get(nextKey).toString() + "}";
			counter++;
		}
		objectJson += "]";
		
		return objectJson;
	}

}
