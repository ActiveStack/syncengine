package com.percero.agents.sync.vo;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


public class RunQueryResponse extends SyncResponse {

	private List<Object> result;
	public List<Object> getResult() {
		return result;
	}
	public void setResult(List<Object> result) {
		this.result = result;
	}

	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":[";

		int counter = 0;
		if (objectMapper == null)
			objectMapper = new ObjectMapper();

		if(result != null)
			for(Object nextObject : getResult()) {
				if (counter > 0)
					objectJson += ",";
				try {
					objectJson += objectMapper.writeValueAsString(nextObject);
					counter++;
				} catch (JsonGenerationException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		objectJson += "]";
		return objectJson;
	}
}
