package gg.projecteden.mongodb.serializers;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import gg.projecteden.utils.StringUtils;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor
public class LocalTimeConverter extends TypeConverter implements SimpleValueConverter {

	public LocalTimeConverter(Mapper mapper) {
		super(LocalTime.class);
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null) return null;
		return DateTimeFormatter.ISO_LOCAL_TIME.format((LocalTime) value);
	}

	@Override
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		return decode(value);
	}

	public LocalTime decode(Object value) {
		if (!(value instanceof String string)) return null;
		if (StringUtils.isNullOrEmpty(string)) return null;
		return LocalTime.parse((String) value);
	}

}
