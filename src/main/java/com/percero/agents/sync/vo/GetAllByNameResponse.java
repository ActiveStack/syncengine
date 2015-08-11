package com.percero.agents.sync.vo;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;


public class GetAllByNameResponse extends SyncResponse {

	private List<BaseDataObject> result;
	public List<BaseDataObject> getResult() {
		return result;
	}
	public void setResult(List<BaseDataObject> result) {
		this.result = result;
	}
	
	private Integer pageSize;
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer value) {
		pageSize = value;
	}
	
	private Integer pageNumber;
	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer value) {
		pageNumber = value;
	}
	
	private Integer totalCount;
	public Integer getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(Integer value) {
		totalCount = value;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",\"result\":[";
		int counter = 0;
		for(BaseDataObject nextBDO : getResult()) {
			if (counter > 0)
				objectJson += ",";
			objectJson += nextBDO.toJson();
			counter++;
		}
		objectJson += "],";
		
		objectJson += "\"pageSize\":" + (getPageSize() == null ? "null" : getPageSize()) + ",";
		objectJson += "\"pageNumber\":" + (getPageNumber() == null ? "null" : getPageNumber()) + ",";
		objectJson += "\"totalCount\":" + (getTotalCount() == null ? "null" : getTotalCount());
		return objectJson;
	}

}
