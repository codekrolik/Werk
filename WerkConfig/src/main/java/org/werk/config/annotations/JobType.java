package org.werk.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JobType {
	String name();
	String description() default "";
	String firstStep();
	String customInfo() default "";
	boolean forceAcyclic() default false;
}
