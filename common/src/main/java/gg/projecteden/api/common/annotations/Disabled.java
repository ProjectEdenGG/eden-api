package gg.projecteden.api.common.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that a class or method is disabled and should not be used.
 * Useful for objects that are loaded via reflection.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Disabled {
}
