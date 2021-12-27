package gg.projecteden.annotations;

import gg.projecteden.utils.Env;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Determines the {@link Env environments} in which a class or method should be enabled.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Environments {
	Env[] value();
}
