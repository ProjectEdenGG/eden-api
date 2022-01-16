package gg.projecteden.mongodb.serializers;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import gg.projecteden.utils.Nullables;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@NoArgsConstructor
public class LocalDateConverter extends TypeConverter implements SimpleValueConverter {
	static final String PATTERN = "M/d/yyyy";

	public LocalDateConverter(Mapper mapper) {
		super(LocalDate.class);
	}

	@Override
	public Object encode(Object value, MappedField optionalExtraInfo) {
		if (value == null) return null;
		return DateTimeFormatter.ISO_LOCAL_DATE.format((LocalDate) value);
	}

	@Override
	public Object decode(Class<?> aClass, Object value, MappedField mappedField) {
		return decode(value);
	}

	public LocalDate decode(Object value) {
		if (!(value instanceof String string)) return null;
		if (Nullables.isNullOrEmpty(string)) return null;

		try {
			return LocalDate.parse(string);
		} catch (DateTimeParseException ex) {
			return LocalDate.parse(string, DateTimeFormatter.ofPattern(PATTERN));
		}
	}

}
