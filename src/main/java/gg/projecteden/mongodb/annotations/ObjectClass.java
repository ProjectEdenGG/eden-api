package gg.projecteden.mongodb.annotations;

import gg.projecteden.interfaces.DatabaseObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectClass {
	Class<? extends DatabaseObject> value();

}
