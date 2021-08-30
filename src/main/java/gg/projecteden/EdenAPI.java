package gg.projecteden;

import com.google.gson.GsonBuilder;
import dev.morphia.converters.TypeConverter;
import gg.projecteden.mongodb.DatabaseConfig;
import gg.projecteden.mongodb.MongoConnector;
import gg.projecteden.utils.Env;

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

	public GsonBuilder getPrettyPrinter() {
		return new GsonBuilder().setPrettyPrinting();
	}

	public void sync(Runnable runnable) {
		runnable.run();
	}

}
