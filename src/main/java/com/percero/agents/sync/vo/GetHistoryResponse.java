package com.percero.agents.sync.vo;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;



public class GetHistoryResponse extends SyncResponse {

	private List<HistoricalObject> result;
	public List<HistoricalObject> getResult() {
		return result;
	}
	public void setResult(List<HistoricalObject> result) {
		this.result = result;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":[";
		int counter = 0;
		for(HistoricalObject nextBDO : getResult()) {
			if (counter > 0)
				objectJson += ",";
			objectJson += nextBDO.toJson(objectMapper);
			counter++;
		}
		objectJson += "]";
		return objectJson;
	}
}
