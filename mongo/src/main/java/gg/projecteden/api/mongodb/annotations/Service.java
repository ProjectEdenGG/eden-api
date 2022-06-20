package gg.projecteden.api.mongodb.annotations;

import gg.projecteden.api.interfaces.DatabaseObject;
import gg.projecteden.api.mongodb.MongoService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
	Class<? extends MongoService<? extends DatabaseObject>> value();

}
