package com.percero.agents.sync.vo;

public class GetAllByNameRequest extends SyncRequest {

	private String theClassName;
	public String getTheClassName() {
		return theClassName;
	}
	public void setTheClassName(String theClassName) {
		this.theClassName = theClassName;
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
	
	private Boolean returnTotal;
	public Boolean getReturnTotal() {
		return returnTotal;
	}
	public void setReturnTotal(Boolean value) {
		returnTotal = value;
	}
}
