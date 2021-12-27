package gg.projecteden.mongodb;

import dev.morphia.converters.TypeConverter;
import gg.projecteden.EdenAPI;

import java.util.Collection;
import java.util.Collections;

public abstract class EdenDatabaseAPI extends EdenAPI {
	@Override
	public void shutdown() {
		MongoConnector.shutdown();
	}

	public Collection<? extends Class<? extends TypeConverter>> getMongoConverters() {
		return Collections.emptyList();
	}
}
