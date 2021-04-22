package me.pugabyte.edenapi;

import me.pugabyte.edenapi.persistence.DatabaseConfig;
import me.pugabyte.edenapi.utils.Env;
import org.apache.commons.lang3.NotImplementedException;

public abstract class EdenAPI {

	public static EdenAPI get() {
		throw new NotImplementedException();
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig(String type);

}
