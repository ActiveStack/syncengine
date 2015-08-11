package com.percero.agents.sync.vo;



public class PushCWUpdateRequest extends SyncRequest {

	private ClassIDPair classIdPair;
	public ClassIDPair getClassIdPair() {
		return classIdPair;
	}
	public void setClassIdPair(ClassIDPair classIdPair) {
		this.classIdPair = classIdPair;
	}
	
	private String fieldName;
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String value) {
		fieldName = value;
	}
	
	private String[] params;
	public String[] getParams() {
		return params;
	}
	public void setParams(String[] value) {
		params = value;
	}

}
