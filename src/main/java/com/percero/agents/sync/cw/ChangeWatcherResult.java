package com.percero.agents.sync.cw;

public class ChangeWatcherResult {

	public ChangeWatcherResult() {
		
	}
	
	public ChangeWatcherResult(Object oldValue) {
		this.oldValue = oldValue;
	}
	
	public ChangeWatcherResult(Object oldValue, Object newValue) {
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public ChangeWatcherResult(Object oldValue, Object newValue, Boolean eqOrBothNull) {
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.eqOrBothNull = eqOrBothNull;
	}
	
	private Object oldValue = null;
	private Object newValue = null;
	private Boolean eqOrBothNull = false;

	public Object getOldValue() {
		return oldValue;
	}

	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	public void setNewValue(Object newValue) {
		this.newValue = newValue;
	}

	public Boolean getEqOrBothNull() {
		return eqOrBothNull;
	}

	public void setEqOrBothNull(Boolean eqOrBothNull) {
		this.eqOrBothNull = eqOrBothNull;
	}

}
