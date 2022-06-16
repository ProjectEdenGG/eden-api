package gg.projecteden.api.mongodb.serializers;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import gg.projecteden.api.common.utils.Nullables;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@NoArgsConstructor
public class LocalTimeConverter extends TypeConverter implements SimpleValueConverter {
	static final String PATTERN = "H:mm:ss a";

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
		if (Nullables.isNullOrEmpty(string)) return null;

		try {
			return LocalTime.parse(string);
		} catch (DateTimeParseException ex) {
			return LocalTime.parse(string, DateTimeFormatter.ofPattern(PATTERN));
		}
	}

}
