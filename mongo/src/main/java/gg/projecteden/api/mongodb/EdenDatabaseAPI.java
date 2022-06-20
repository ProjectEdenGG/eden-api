package gg.projecteden.api.mongodb;

import dev.morphia.converters.TypeConverter;
import gg.projecteden.api.common.EdenAPI;

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
