package com.percero.agents.auth.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IUserIdentifierA {

	String userAnchorFieldName() default "userAnchor";

	String userAnchorFieldType();

	String userIdentifierParadigm() default "email";
	
	String userIdentifierFieldName() default "userIdentifier";

	String userIdentifierFieldType() default "String";
	
}
