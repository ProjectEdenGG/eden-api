package gg.projecteden.utils;

import gg.projecteden.exceptions.EdenException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gg.projecteden.utils.Nullables.isNullOrEmpty;

public class TimeUtils {

	public static String longDateTimeFormat(LocalDateTime dateTime) {
		if (dateTime == null) return null;
		return longDateFormat(dateTime.toLocalDate()) + " " + longTimeFormat(dateTime);
	}

	public static String shortDateTimeFormat(LocalDateTime dateTime) {
		if (dateTime == null) return null;
		return shortDateFormat(dateTime.toLocalDate()) + " " + shortTimeFormat(dateTime);
	}

	public static String shortishDateTimeFormat(LocalDateTime dateTime) {
		if (dateTime == null) return null;
		return shortishDateFormat(dateTime.toLocalDate()) + " " + shortishTimeFormat(dateTime);
	}

	public static String longDateFormat(LocalDate date) {
		if (date == null) return null;
		return StringUtils.camelCase(date.getMonth().name()) + " " + StringUtils.getNumberWithSuffix(date.getDayOfMonth()) + ", " + date.getYear();
	}

	public static String shortDateFormat(LocalDate date) {
		if (date == null) return null;
		return date.format(DateTimeFormatter.ofPattern("M/d/yy"));
	}

	public static String shortishDateFormat(LocalDate date) {
		if (date == null) return null;
		return date.format(DateTimeFormatter.ofPattern("MM/dd/yy"));
	}

	public static String dateFormat(LocalDate date) {
		if (date == null) return null;
		return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
	}

	public static String longTimeFormat(LocalDateTime time) {
		if (time == null) return null;
		return time.format(DateTimeFormatter.ofPattern("h:mm:ss a"));
	}

	public static String shortTimeFormat(LocalDateTime time) {
		if (time == null) return null;
		return time.format(DateTimeFormatter.ofPattern("h:mm a"));
	}

	public static String shortishTimeFormat(LocalDateTime time) {
		if (time == null) return null;
		return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
	}

	public static LocalDate parseShortDate(String input) {
		return LocalDate.from(DateTimeFormatter.ofPattern("M/d/yyyy").parse(input));
	}

	public static LocalDate parseShorterDate(String input) {
		return LocalDate.from(DateTimeFormatter.ofPattern("M/d/yy").parse(input));
	}

	public static LocalDate parseDate(String input) {
		try {
			return parseShorterDate(input);
		} catch (DateTimeParseException ignore) {
		}
		try {
			return parseShortDate(input);
		} catch (DateTimeParseException ignore) {
		}
		try {
			return LocalDate.parse(input);
		} catch (DateTimeParseException ignore) {
		}
		throw new EdenException("Could not parse date, correct format is MM/DD/YYYY");
	}

	public static LocalDateTime parseDateTime(String input) {
		try {
			return LocalDateTime.parse(input);
		} catch (DateTimeParseException ignore) {
		}
		throw new EdenException("Could not parse date, correct format is YYYY-MM-DDTHH:MM:SS");
	}

	@Getter
	@AllArgsConstructor
	public enum TimespanElement {
		YEAR("y", "yr", "year"),
		WEEK("w", null, "week"),
		DAY("d", null, "day"),
		HOUR("h", "hr", "hour"),
		MINUTE("m", "min", "minute"),
		SECOND("s", "sec", "second"),
		MILLIS("ms", "milli", "millisecond"),
		;

		private final String shortLabel, mediumLabel, longLabel;

		public long of(String input) {
			try {
				double multiplier = Double.parseDouble(input.replaceAll("[^\\d.]+", ""));
				return MillisTime.valueOf(name()).x(multiplier);
			} catch (NumberFormatException ex) {
				throw new EdenException("Invalid " + name().toLowerCase() + ": &e" + input);
			}
		}

		public Pattern getPattern() {
			return Pattern.compile("(?i)\\d+(\\.\\d+)?( )?(" + longLabel + "(s)?|" + (mediumLabel == null ? "" : mediumLabel + "(s)?|") + shortLabel + ")");
		}

		public static Pattern getAllPattern() {
			StringBuilder regex = new StringBuilder();
			for (TimespanElement element : values())
				regex.append("([tT]:)?(").append(element.getPattern().pattern()).append("( )?){0,}");
			return Pattern.compile(regex.toString());
		}
	}

	@ToString
	public static class Timespan {
		@Getter
		private final long original;
		private final boolean noneDisplay, displayMillis;
		private final FormatType formatType;
		private long years, days, hours, minutes, seconds, millis;
		@Getter
		private final String rest;

		@Builder
		public Timespan(long millis, boolean noneDisplay, boolean displayMillis, FormatType formatType, String rest) {
			this.original = millis;
			this.millis = millis;
			this.noneDisplay = noneDisplay;
			this.displayMillis = displayMillis;
			this.formatType = formatType == null ? FormatType.SHORT : formatType;
			this.rest = rest;
			calculate();
		}

		public static Timespan of(LocalDate from) {
			return TimespanBuilder.of(from).build();
		}

		public static Timespan of(LocalDateTime from) {
			return TimespanBuilder.of(from).build();
		}

		public static Timespan of(LocalDateTime from, LocalDateTime to) {
			return TimespanBuilder.of(from, to).build();
		}

		public static Timespan ofSeconds(long seconds) {
			return TimespanBuilder.ofSeconds(seconds).build();
		}

		public static Timespan ofSeconds(int seconds) {
			return TimespanBuilder.ofSeconds(seconds).build();
		}

		public static Timespan ofMillis(long millis) {
			return TimespanBuilder.ofMillis(millis).build();
		}

		public static Timespan ofMillis(int millis) {
			return TimespanBuilder.ofMillis(millis).build();
		}

		public static Timespan of(String input) {
			return TimespanBuilder.of(input).build();
		}

		public static Timespan find(String input) {
			return TimespanBuilder.find(input).build();
		}

		public static class TimespanBuilder {

			public static TimespanBuilder of(LocalDate from) {
				return of(from.atStartOfDay());
			}

			public static TimespanBuilder of(LocalDateTime from) {
				LocalDateTime now = LocalDateTime.now();
				if (from.isBefore(now))
					return of(from, now);
				else
					return of(now, from);
			}

			public static TimespanBuilder of(LocalDateTime from, LocalDateTime to) {
				return ofMillis(from.until(to, ChronoUnit.MILLIS));
			}

			public static TimespanBuilder ofSeconds(long seconds) {
				return Timespan.builder().millis(seconds * 1000);
			}

			public static TimespanBuilder ofSeconds(int seconds) {
				return ofSeconds(Integer.valueOf(seconds).longValue());
			}

			public static TimespanBuilder ofMillis(long millis) {
				return Timespan.builder().millis(millis);
			}

			public static TimespanBuilder ofMillis(int millis) {
				return ofMillis(Integer.valueOf(millis).longValue());
			}

			public static TimespanBuilder of(String input) {
				if (isNullOrEmpty(input))
					return ofMillis(0);

				input = input.replaceFirst("[tT]:", "");
				if (Utils.isLong(input))
					return ofSeconds(Long.parseLong(input));

				long millis = 0;
				for (TimespanElement element : TimespanElement.values()) {
					Matcher matcher = element.getPattern().matcher(input);

					while (matcher.find())
						millis += element.of(matcher.group());
				}

				return ofMillis(millis);
			}

			public static TimespanBuilder find(String input) {
				if (!isNullOrEmpty(input)) {
					Matcher matcher = TimespanElement.getAllPattern().matcher(input);
					while (matcher.find()) {
						String group = matcher.group();
						if (group.trim().length() == 0) continue;
						return of(group).rest(input.replaceFirst(group, "").trim());
					}
				}

				return ofSeconds(0).rest(input);
			}

			public TimespanBuilder displayMillis() {
				this.displayMillis = true;
				return this;
			}

			@ToString.Include
			public String format() {
				return format(formatType);
			}

			public String format(FormatType formatType) {
				return build().format(formatType);
			}

		}

		private void calculate() {
			if (millis == 0) return;

			years = millis / 1000 / 60 / 60 / 24 / 365;
			millis -= years * 1000 * 60 * 60 * 24 * 365;
			days = millis / 1000 / 60 / 60 / 24;
			millis -= days * 1000 * 60 * 60 * 24;
			hours = millis / 1000 / 60 / 60;
			millis -= hours * 1000 * 60 * 60;
			minutes = millis / 1000 / 60;
			millis -= minutes * 1000 * 60;
			seconds = millis / 1000;
			millis -= seconds * 1000;
		}

		public LocalDateTime fromNow() {
			return LocalDateTime.now().plus(original, ChronoUnit.MILLIS);
		}

		public LocalDateTime sinceNow() {
			return LocalDateTime.now().minus(original, ChronoUnit.MILLIS);
		}

		public boolean isNull() {
			return original == 0;
		}

		public String format() {
			return format(formatType);
		}

		public String format(FormatType formatType) {
			formatType = formatType == null ? FormatType.SHORT : formatType;
			if (isNull() && noneDisplay)
				return "None";

			long years = this.years;
			long days = this.days;
			if (formatType == FormatType.SHORT_NO_YEARS) {
				days += years * 365;
				years = 0;
			}

			String result = "";
			if (years > 0)
				result += years + formatType.get(TimespanElement.YEAR, years);
			if (days > 0)
				result += days + formatType.get(TimespanElement.DAY, days);
			if (hours > 0)
				result += hours + formatType.get(TimespanElement.HOUR, hours);
			if (minutes > 0)
				result += minutes + formatType.get(TimespanElement.MINUTE, minutes);
			if (result.length() == 0 || (years == 0 && days == 0 && hours == 0)) {
				if (displayMillis && millis > 0)
					result += seconds + new DecimalFormat(".000").format(millis / 1000d) + formatType.get(TimespanElement.SECOND, seconds);
				else
					result += seconds + formatType.get(TimespanElement.SECOND, seconds);
			}

			return result.trim();
		}

		public enum FormatType {
			SHORT {
				@Override
				public String get(TimespanElement label, long value) {
					return label.getShortLabel() + " ";
				}
			},
			MEDIUM {
				@Override
				public String get(TimespanElement label, long value) {
					return " " + StringUtils.plural(label.getMediumLabel() == null ? label.getLongLabel() : label.getMediumLabel(), value) + " ";
				}
			},
			LONG {
				@Override
				public String get(TimespanElement label, long value) {
					return " " + StringUtils.plural(label.getLongLabel(), value) + " ";
				}
			},
			SHORT_NO_YEARS {
				@Override
				public String get(TimespanElement label, long value) {
					return SHORT.get(label, value);
				}
			};

			abstract String get(TimespanElement label, long value);
		}

	}

	private interface TimeEnum {

		long get();

		default long x(int multiplier) {
			return get() * multiplier;
		}

		default long x(double multiplier) {
			return (long) (get() * multiplier);
		}

		default Duration duration(long multiplier) {
			return Duration.ofSeconds(get()).dividedBy(20).multipliedBy(multiplier);
		}

		/**
		 * Duration of a fraction.
		 *
		 * @param numerator   fraction top half
		 * @param denominator fraction bottom half
		 */
		default Duration duration(long numerator, long denominator) {
			return duration(numerator).dividedBy(denominator);
		}

	}

	@AllArgsConstructor
	public enum MillisTime implements TimeEnum {
		MILLISECOND(1),
		SECOND(MILLISECOND.get() * 1000),
		MINUTE(SECOND.get() * 60),
		HOUR(MINUTE.get() * 60),
		DAY(HOUR.get() * 24),
		WEEK(DAY.get() * 7),
		MONTH(DAY.get() * 30),
		YEAR(DAY.get() * 365);

		private final long value;

		public long get() {
			return value;
		}

	}

	@AllArgsConstructor
	public enum TickTime implements TimeEnum {
		TICK(1),
		SECOND(TICK.get() * 20),
		MINUTE(SECOND.get() * 60),
		HOUR(MINUTE.get() * 60),
		DAY(HOUR.get() * 24),
		WEEK(DAY.get() * 7),
		MONTH(DAY.get() * 30),
		YEAR(DAY.get() * 365);

		private final long value;

		public long get() {
			return value;
		}

	}

}
