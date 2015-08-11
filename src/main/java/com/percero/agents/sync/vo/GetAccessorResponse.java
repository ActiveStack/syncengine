package com.percero.agents.sync.vo;

import org.codehaus.jackson.map.ObjectMapper;

import com.percero.framework.accessor.IAccessor;


public class GetAccessorResponse extends SyncResponse {

	private BaseDataObject resultObject;
	public BaseDataObject getResultObject() {
		return resultObject;
	}
	public void setResultObject(BaseDataObject resultObject) {
		this.resultObject = resultObject;
	}

	private IAccessor accessor;
	public IAccessor getAccessor() {
		return accessor;
	}
	public void setAccessor(IAccessor accessor) {
		this.accessor = accessor;
	}
	
	@Override
	public String retrieveBaseJson(ObjectMapper objectMapper) {
		String objectJson = super.retrieveBaseJson(objectMapper) + ",";
		objectJson += "\"resultObject\":" + (getResultObject() == null ? "null" : getResultObject().toJson(objectMapper)) + ",";
		objectJson += "\"accessor\":" + (getAccessor() == null ? "null" : "{" + getAccessor().retrieveJson(objectMapper) + "}") ;
		return objectJson;
	}
}
