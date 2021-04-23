package eden;

import dev.morphia.converters.TypeConverter;
import eden.mongodb.DatabaseConfig;
import eden.mongodb.MongoConnector;
import eden.utils.Env;

import java.util.Collection;
import java.util.Collections;

public abstract class EdenAPI {
	protected static EdenAPI instance;

	public static EdenAPI get() {
		return instance;
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig();

	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	public Collection<? extends Class<? extends TypeConverter>> getMongoConverters() {
		return Collections.emptyList();
	}

	public static void shutdown() {
		MongoConnector.shutdown();
	}

}
