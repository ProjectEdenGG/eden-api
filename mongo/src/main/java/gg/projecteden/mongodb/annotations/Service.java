package gg.projecteden.mongodb.annotations;

import gg.projecteden.interfaces.DatabaseObject;
import gg.projecteden.mongodb.MongoService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
	Class<? extends MongoService<? extends DatabaseObject>> value();

}
