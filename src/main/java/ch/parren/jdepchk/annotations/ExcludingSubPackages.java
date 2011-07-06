package ch.parren.jdepchk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Together with {@link Checked}, signals to JDepChk that the given package is a
 * component definition that does not include subpackages (so it's a "xy.*"
 * instead of a "xy.**" component).
 */
@Target({ ElementType.PACKAGE })//
@Retention(RetentionPolicy.CLASS)//
public @interface ExcludingSubPackages {}
