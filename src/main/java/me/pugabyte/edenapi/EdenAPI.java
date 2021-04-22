package me.pugabyte.edenapi;

import me.pugabyte.edenapi.persistence.DatabaseConfig;
import me.pugabyte.edenapi.persistence.DatabaseType;
import me.pugabyte.edenapi.utils.Env;
import org.apache.commons.lang3.NotImplementedException;

public abstract class EdenAPI {
	protected static EdenAPI instance;

	public static EdenAPI api() {
		return instance;
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig(DatabaseType type);

}
