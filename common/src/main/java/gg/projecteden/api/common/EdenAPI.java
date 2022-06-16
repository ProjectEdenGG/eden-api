package gg.projecteden.api.common;

import com.google.gson.GsonBuilder;
import gg.projecteden.api.common.utils.Env;

import java.util.Optional;
import java.util.UUID;

public abstract class EdenAPI {
	protected static EdenAPI instance;

	public static EdenAPI get() {
		return instance;
	}

	public static <T extends EdenAPI> Optional<T> get(Class<T> clazz) {
		if (clazz.isInstance(instance)) {
			//noinspection unchecked
			return Optional.of((T) instance);
		}
		return Optional.empty();
	}

	public String getAppName() {
		return getClass().getSimpleName();
	}

	public UUID getAppUuid() {
		return UUID.nameUUIDFromBytes(getAppName().getBytes());
	}

	abstract public Env getEnv();

	abstract public DatabaseConfig getDatabaseConfig();

	public ClassLoader getClassLoader() {
		return getClass().getClassLoader();
	}

	public abstract void shutdown();

	public GsonBuilder getPrettyPrinter() {
		return new GsonBuilder().setPrettyPrinting();
	}

	public void sync(Runnable runnable) {
		runnable.run();
	}

}
