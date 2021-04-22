package me.pugabyte.edenapi;

import me.pugabyte.edenapi.mongodb.DatabaseConfig;
import me.pugabyte.edenapi.mongodb.MongoDBPersistence;
import me.pugabyte.edenapi.utils.Env;

public abstract class EdenAPI {
	protected static EdenAPI instance;

	public static EdenAPI api() {
		return instance;
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig();

	public void shutdown() {
		MongoDBPersistence.shutdown();
	}

}
