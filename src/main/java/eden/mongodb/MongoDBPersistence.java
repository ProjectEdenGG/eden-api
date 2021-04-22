package eden.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.Builder;
import eden.mongodb.serializers.BigDecimalConverter;
import eden.mongodb.serializers.LocalDateConverter;
import eden.mongodb.serializers.LocalDateTimeConverter;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.Log;
import eden.utils.StringUtils;
import lombok.SneakyThrows;
import org.reflections.Reflections;

public abstract class MongoDBPersistence {
	protected static final Morphia morphia = new Morphia();
	private static Datastore datastore;

	@SneakyThrows
	private static Datastore openConnection(DatabaseConfig config) {
		return build(config);
	}

	private static Datastore build(DatabaseConfig config) {
		return build(config, MapperOptions.builder());
	}

	private static Datastore build(DatabaseConfig config, Builder options) {
		// Properly merge deleted hashmaps and null vars
		options.storeEmpties(true).storeNulls(true);
		morphia.getMapper().setOptions(options.build());

		// Load classes into memory once
		if (!StringUtils.isNullOrEmpty(config.getModelPath()))
			new Reflections(config.getModelPath()).getTypesAnnotatedWith(Entity.class);

		MongoCredential root = MongoCredential.createScramSha1Credential(config.getUsername(), "admin", config.getPassword().toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress(), root, MongoClientOptions.builder().build());
		Datastore datastore = morphia.createDatastore(mongoClient, (config.getPrefix() == null ? "" : config.getPrefix() + "_") + "bearnation");
		morphia.getMapper().getConverters().addConverter(new BigDecimalConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new LocalDateConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new LocalDateTimeConverter(morphia.getMapper()));
		morphia.getMapper().getConverters().addConverter(new UUIDConverter(morphia.getMapper()));
		datastore.ensureIndexes();
		return datastore;
	}

	public static Datastore getConnection(DatabaseConfig config) {
		try {
			if (datastore == null)
				datastore = openConnection(config);
			return datastore;
		} catch (Exception ex) {
			Log.severe("Could not establish connection to MongoDB: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
	}

	public static void shutdown() {
		try {
			if (datastore != null) {
				datastore.getMongo().close();
				datastore = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


}
