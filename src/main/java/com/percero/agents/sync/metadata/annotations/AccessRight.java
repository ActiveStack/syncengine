package com.percero.agents.sync.metadata.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({TYPE}) 
@Retention(RUNTIME)
//@Target(ElementType.FIELD)
public @interface AccessRight {
	String type();
	String query();
}
