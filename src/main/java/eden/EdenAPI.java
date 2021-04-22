package eden;

import eden.mongodb.DatabaseConfig;
import eden.mongodb.MongoDBPersistence;
import eden.utils.Env;

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
