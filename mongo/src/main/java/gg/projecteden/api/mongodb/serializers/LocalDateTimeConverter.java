package gg.projecteden.api.mongodb.serializers;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import gg.projecteden.api.common.utils.Nullables;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@NoArgsConstructor
public class LocalDateTimeConverter extends TypeConverter implements SimpleValueConverter {
	static final String PATTERN = LocalDateConverter.PATTERN + " " + LocalTimeConverter.PATTERN;

	public LocalDateTimeConverter(Mapper mapper) {
		super(LocalDateTime.class);
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		return toEncoded(value);
	}

	public static Object toEncoded(Object value) {
		if (!(value instanceof LocalDateTime)) return null;
		return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format((LocalDateTime) value);
	}

	@Override
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		return decode(value);
	}

	public static LocalDateTime decode(Object value) {
		if (!(value instanceof String string)) return null;
		if (Nullables.isNullOrEmpty(string)) return null;

		try {
			return LocalDateTime.parse(string);
		} catch (DateTimeParseException ex) {
			return LocalDateTime.parse(string, DateTimeFormatter.ofPattern(PATTERN));
		}
	}

}
