package com.percero.agents.auth.vo;

import com.percero.framework.vo.IPerceroObject;

public interface IUserRole extends IPerceroObject {

	public static final String DEFAULT_CLASS_NAME = "UserRole";
	public static final String USER_ANCHOR_FIELD_NAME = "userAnchor";
	public static final String ROLE_NAME_FIELD_NAME = "roleName";
	public static final String USER_ROLES_FIELD_NAME = "roles";
	
	public String getRoleName();
	public void setRoleName(String value);
	
	/**
	public IUserAnchor getUserAnchor();
	public void setUserAnchor(IUserAnchor value);
	public String getUserAnchorFieldName();**/
}