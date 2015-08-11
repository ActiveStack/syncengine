package com.percero.agents.sync.vo;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;


public class SearchByExampleResponse extends SyncResponse {

	private List<BaseDataObject> result;
	public List<BaseDataObject> getResult() {
		return result;
	}
	public void setResult(List<BaseDataObject> result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":[";
		int counter = 0;
		for(BaseDataObject nextBDO : result) {
			if (counter > 0)
				objectJson += ",";
			objectJson += nextBDO.toJson(objectMapper);
			counter++;
		}
		objectJson += "]";
		return objectJson;
	}
}
