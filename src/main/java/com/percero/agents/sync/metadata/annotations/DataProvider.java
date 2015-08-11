package com.percero.agents.sync.metadata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.FIELD)
public @interface DataProvider {
	String name() default "";
	String objectType() default "";
}
