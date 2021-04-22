package me.pugabyte.edenapi.persistence;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.Builder;
import lombok.SneakyThrows;
import me.pugabyte.edenapi.persistence.serializer.mongodb.BigDecimalConverter;
import me.pugabyte.edenapi.persistence.serializer.mongodb.LocalDateConverter;
import me.pugabyte.edenapi.persistence.serializer.mongodb.LocalDateTimeConverter;
import me.pugabyte.edenapi.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.edenapi.utils.Log;
import me.pugabyte.edenapi.utils.StringUtils;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;

public abstract class MongoDBPersistence {
	protected static final Morphia morphia = new Morphia();
	private static Map<MongoDBDatabase, Datastore> databases = new HashMap<>();

	@SneakyThrows
	private static Datastore openConnection(MongoDBDatabase dbType, DatabaseConfig config) {
		return build(dbType, config);
	}

	private static Datastore build(MongoDBDatabase dbType, DatabaseConfig config) {
		return build(dbType, config, MapperOptions.builder());
	}

	private static Datastore build(MongoDBDatabase dbType, DatabaseConfig config, Builder options) {
		// Properly merge deleted hashmaps and null vars
		options.storeEmpties(true).storeNulls(true);
		morphia.getMapper().setOptions(options.build());

		// Load classes into memory once
		if (!StringUtils.isNullOrEmpty(config.getModelPath()))
			new Reflections(config.getModelPath()).getTypesAnnotatedWith(Entity.class);

		MongoCredential root = MongoCredential.createScramSha1Credential(config.getUsername(), "admin", config.getPassword().toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress(), root, MongoClientOptions.builder().build());
		Datastore datastore = morphia.createDatastore(mongoClient, config.getPrefix() + dbType.getDatabase());
		morphia.getMapper().getConverters().addConverter(new BigDecimalConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new LocalDateConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new LocalDateTimeConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new UUIDConverter(morphia.getMapper()));
		return datastore;
	}

	public static Datastore getConnection(MongoDBDatabase db, DatabaseConfig config) {
		try {
			return databases.computeIfAbsent(db, $ -> openConnection(db, config));
		} catch (Exception ex) {
			Log.severe("Could not establish connection to the MongoDB \"" + db.getDatabase() + "\" database: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
	}

	public static void shutdown() {
		databases.values().forEach(datastore -> {
			try {
				datastore.getMongo().close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}


}
