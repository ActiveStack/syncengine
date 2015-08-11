package com.percero.agents.auth.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IUserAnchorA {

	String userRolesFieldName() default "userRoles";
	String userRolesFieldType() default "[unassigned]";

	String userIdentifiersFieldName() default "userIdentifiers";
	String userIdentifiersFieldType() default "[unassigned]";
	
	String userIdFieldName() default "userId";
	String userIdFieldType() default "String";
	
	String firstNameFieldName() default "firstName";
	String firstNameFieldType() default "String";
	String lastNameFieldName() default "lastName";
	String lastNameFieldType() default "String";
}
