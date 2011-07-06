package ch.parren.jdepchk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.ANNOTATION_TYPE })//
@Retention(RetentionPolicy.CLASS)//
public @interface Extends {
	String[] value();
}
