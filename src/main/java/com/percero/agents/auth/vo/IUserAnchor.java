package com.percero.agents.auth.vo;

public interface IUserAnchor {

	public static final String FIRST_NAME_FIELD = "firstName";
	public static final String LAST_NAME_FIELD = "lastName";
	public static final String USER_ID_FIELD = "userId";
	public static final String DEFAULT_CLASS_NAME = "UserAnchor";
	
	public String getUserId();
	public void setUserId(String value);

//	public java.util.List<com.com.percero.auth.vo.IUserRole> getUserRoles();
//	public void setUserRoles(java.util.List<com.com.percero.auth.vo.IUserRole> value);
}
