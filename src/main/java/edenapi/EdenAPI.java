package edenapi;

import edenapi.mongodb.DatabaseConfig;
import edenapi.mongodb.MongoDBPersistence;
import edenapi.utils.Env;

public abstract class EdenAPI {
	protected static EdenAPI instance;

	public static EdenAPI get() {
		return instance;
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig();

	public void shutdown() {
		MongoDBPersistence.shutdown();
	}

}
