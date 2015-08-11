package com.percero.agents.auth.vo;

import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="cn")
public interface IAuthCachedObject {
	public String getID();
	public void setID(String value);
}
