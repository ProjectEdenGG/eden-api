package eden.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.annotations.Entity;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.Builder;
import eden.EdenAPI;
import eden.mongodb.serializers.BigDecimalConverter;
import eden.mongodb.serializers.LocalDateConverter;
import eden.mongodb.serializers.LocalDateTimeConverter;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.StringUtils;
import lombok.Getter;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class MongoConnector {
	protected static final Morphia morphia = new Morphia();
	@Getter
	private static Datastore datastore;

	public static Datastore connect() {
		if (datastore != null)
			return datastore;

		// Properly merge deleted hashmaps and null vars
		Builder options = MapperOptions.builder().storeEmpties(true).storeNulls(true).classLoader(EdenAPI.get().getClassLoader());
		morphia.getMapper().setOptions(options.build());

		DatabaseConfig config = EdenAPI.get().getDatabaseConfig();
		// Load classes into memory once
		if (!StringUtils.isNullOrEmpty(config.getModelPath()))
			new Reflections(config.getModelPath()).getTypesAnnotatedWith(Entity.class);

		MongoCredential root = MongoCredential.createScramSha1Credential(config.getUsername(), "admin", config.getPassword().toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress(), root, MongoClientOptions.builder().build());
		String database = (config.getPrefix() == null ? "" : config.getPrefix() + "_") + "bearnation";
		datastore = morphia.createDatastore(mongoClient, database);
		datastore.ensureIndexes();

		List<? extends Class<? extends TypeConverter>> classes = new ArrayList<Class<? extends TypeConverter>>() {{
			add(BigDecimalConverter.class);
			add(LocalDateConverter.class);
			add(LocalDateTimeConverter.class);
			add(UUIDConverter.class);
			addAll(EdenAPI.get().getMongoConverters());
		}};

		for (Class<? extends TypeConverter> clazz : classes) {
			try {
				Constructor<? extends TypeConverter> constructor = clazz.getDeclaredConstructor(Mapper.class);
				TypeConverter instance = constructor.newInstance(morphia.getMapper());
				morphia.getMapper().getConverters().addConverter(instance);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return datastore;
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