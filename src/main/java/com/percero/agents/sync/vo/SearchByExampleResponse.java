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
		StringBuilder objectJson = new StringBuilder(super.retrieveBaseJson(objectMapper)).append(",\"result\":[");
		int counter = 0;
		for(BaseDataObject nextBDO : result) {
			if (counter > 0)
				objectJson.append(',');
			objectJson.append(nextBDO.toJson(objectMapper));
			counter++;
		}
		objectJson.append(']');
		return objectJson.toString();
	}
}
