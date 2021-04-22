package me.pugabyte.edenapi.persistence.annotations;

import me.pugabyte.edenapi.models.PlayerOwnedObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlayerClass {
	Class<? extends PlayerOwnedObject> value();
}
