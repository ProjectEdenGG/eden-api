package gg.projecteden.mongodb.serializers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import gg.projecteden.models.scheduledjobs.common.AbstractJob;
import gg.projecteden.mongodb.MongoPlayerService;
import lombok.SneakyThrows;

public class JobConverter extends TypeConverter implements SimpleValueConverter {

	public JobConverter(Mapper mapper) {
		super(AbstractJob.class);
	}

	@Override
	@SneakyThrows
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null) return null;
		final DBObject dbObject = MongoPlayerService.serialize(value);
		dbObject.put("className", value.getClass().getName());
		return dbObject;
	}

	@Override
	@SneakyThrows
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		if (value == null) return null;
		return MongoPlayerService.deserialize((BasicDBObject) value);
	}

}
