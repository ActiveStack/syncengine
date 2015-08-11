package com.percero.agents.auth.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IUserRoleA {

	String userAnchorFieldName();
	String userAnchorFieldType();

	String roleNameFieldName();
	String roleNameFieldType();
	
}
