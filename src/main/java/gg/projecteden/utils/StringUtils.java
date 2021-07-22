package gg.projecteden.utils;

import com.google.gson.Gson;
import gg.projecteden.EdenAPI;
import gg.projecteden.exceptions.EdenException;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StringUtils {

	@Getter
	private final static UUID UUID0 = new UUID(0, 0);

	public static boolean isUUID0(UUID uuid) {
		return UUID0.equals(uuid);
	}

	public static String getDiscordPrefix(String prefix) {
		return "**[" + prefix + "]** ";
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.trim().isEmpty();
	}

	public static int countUpperCase(String s) {
		return (int) s.codePoints().filter(c -> c >= 'A' && c <= 'Z').count();
	}

	public static int countLowerCase(String s) {
		return (int) s.codePoints().filter(c -> c >= 'a' && c <= 'z').count();
	}

	public static String plural(String label, Number number) {
		return label + (number.doubleValue() == 1 ? "" : "s");
	}

	public static String plural(String labelSingle, String labelPlural, Number number) {
		return number.doubleValue() == 1 ? labelSingle : labelPlural;
	}

	public static String trimFirst(String string) {
		return string.substring(1);
	}

	public static String right(String string, int number) {
		return string.substring(Math.max(string.length() - number, 0));
	}

	public static String left(String string, int number) {
		return string.substring(0, Math.min(number, string.length()));
	}

	public static @NotNull String camelCase(Enum<?> _enum) {
		if (_enum == null) return "null";
		return camelCase(_enum.name());
	}

	@Contract("null -> null; !null -> !null")
	public static String camelCase(String text) {
		if (isNullOrEmpty(text))
			return text;

		return Arrays.stream(text.replaceAll("_", " ").split(" "))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public static String camelCaseWithUnderscores(String text) {
		if (isNullOrEmpty(text))
			return text;

		return Arrays.stream(text.split("_"))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
				.collect(Collectors.joining("_"));
	}

	public static String asOxfordList(List<String> items, String separator) {
		if (!separator.contains(", "))
			throw new EdenException("Separator must contain ', '");

		String message = String.join(separator, items);
		int commaIndex = message.lastIndexOf(", ");
		message = new StringBuilder(message).replace(commaIndex, commaIndex + 2, (items.size() > 2 ? "," : "") + " and ").toString();
		return message;
	}

	public static String listFirst(String string, String delimiter) {
		return string.split(delimiter)[0];
	}

	public static String listLast(String string, String delimiter) {
		return string.substring(string.lastIndexOf(delimiter) + 1);
	}

	public static String listGetAt(String string, int index, String delimiter) {
		String[] split = string.split(delimiter);
		return split[index - 1];
	}

	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String uuidFormat(String uuid) {
		uuid = uuidUnformat(uuid);
		String formatted = "";
		formatted += uuid.substring(0, 8) + "-";
		formatted += uuid.substring(8, 12) + "-";
		formatted += uuid.substring(12, 16) + "-";
		formatted += uuid.substring(16, 20) + "-";
		formatted += uuid.substring(20, 32);
		return formatted;
	}

	public static String uuidUnformat(String uuid) {
		return uuid.replaceAll("-", "");
	}

	public static final String UUID_REGEX = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";

	public static boolean isUuid(String uuid) {
		return uuid != null && uuid.matches(UUID_REGEX);
	}

	public static boolean isV4Uuid(UUID uuid) {
		return isV4Uuid(uuid.toString());
	}

	public static boolean isV4Uuid(String uuid) {
		return uuid.charAt(14) == '4';
	}

	public static boolean isValidJson(String json) {
		try {
			new JSONObject(json);
		} catch (JSONException ex) {
			try {
				new JSONArray(json);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	private static final Gson prettyPrinter = EdenAPI.get().getPrettyPrinter().create();

	public static String toPrettyString(Object object) {
		if (object == null) return null;
		try {
			return prettyPrinter.toJson(object);
		} catch (Exception | StackOverflowError ignored) {
			return object.toString();
		}
	}

	protected static final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();

	public static String pretty(Number number) {
		String format = trimFirst(moneyFormat.format(number));
		if (format.endsWith(".00"))
			format = left(format, format.length() - 3);

		return format;
	}

	public static String prettyMoney(Number number) {
		if (number == null)
			return null;
		return "$" + pretty(number);
	}

	public static String stripTrailingZeros(String number) {
		return number.contains(".") ? number.replaceAll("0*$", "").replaceAll("\\.$", "") : number;
	}

	// Attempt to strip symbols and support euro formatting
	public static String asParsableDecimal(String value) {
		if (value == null)
			return "0";

		value = value.replace("$", "");
		if (value.contains(",") && value.contains("."))
			if (value.indexOf(",") < value.indexOf("."))
				value = value.replaceAll(",", "");
			else {
				value = value.replaceAll("\\.", "");
				value = value.replaceAll(",", ".");
			}
		else if (value.contains(",") && value.indexOf(",") == value.lastIndexOf(","))
			if (value.indexOf(",") == value.length() - 3)
				value = value.replace(",", ".");
			else
				value = value.replace(",", "");
		return value;
	}

	public static String ellipsis(String text, int length) {
		if (text.length() > length)
			return text.substring(0, length) + "...";
		else
			return text;
	}

	public static String distanceMetricFormat(int cm) {
		int original = cm;
		int km = cm / 1000 / 100;
		cm -= km * 1000 * 100;
		int meters = cm / 100;
		cm -= meters * 100;

		String result = "";
		if (km > 0)
			result += km + "km ";
		if (meters > 0)
			result += meters + "m ";

		if (result.length() > 0)
			return result.trim();
		else
			return original + "cm";
	}

	public static String getNumberWithSuffix(int number) {
		String text = String.valueOf(number);
		if (text.endsWith("1"))
			if (text.endsWith("11"))
				return number + "th";
			else
				return number + "st";
		else if (text.endsWith("2"))
			if (text.endsWith("12"))
				return number + "th";
			else
				return number + "nd";
		else if (text.endsWith("3"))
			if (text.endsWith("13"))
				return number + "th";
			else
				return number + "rd";
		else
			return number + "th";
	}

	@Getter
	protected static final DecimalFormat df = new DecimalFormat("#.00");

	@Getter
	protected static final DecimalFormat nf = new DecimalFormat("#");

	@Getter
	protected static final DecimalFormat cdf = new DecimalFormat("#,###.00"); // comma decimal formatter

	@Getter
	protected static final DecimalFormat cnf = new DecimalFormat("#,###"); // comma number formatter

	public static DecimalFormat getFormatter(Class<?> type) {
		if (Integer.class == type || Integer.TYPE == type) return nf;
		if (Double.class == type || Double.TYPE == type) return df;
		if (Float.class == type || Float.TYPE == type) return df;
		if (Short.class == type || Short.TYPE == type) return nf;
		if (Long.class == type || Long.TYPE == type) return nf;
		if (Byte.class == type || Byte.TYPE == type) return nf;
		if (BigDecimal.class == type) return df;
		throw new EdenException("No formatter found for class " + type.getSimpleName());
	}

	private static final String HASTEBIN = "https://paste.projecteden.gg/";

	@Data
	private static class PasteResult {
		private String key;

	}

	@SneakyThrows
	public static String paste(String content) {
		Request request = new Request.Builder().url(HASTEBIN + "documents").post(RequestBody.create(MediaType.get("text/plain"), content)).build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			PasteResult result = new Gson().fromJson(response.body().string(), PasteResult.class);
			return HASTEBIN + result.getKey();
		}
	}

	@NonNull
	public static String getPaste(String code) {
		try {
			Request request = new Request.Builder().url(HASTEBIN + "raw/" + code).get().build();
			try (Response response = new OkHttpClient().newCall(request).execute()) {
				return response.body().string();
			}
		} catch (Exception ex) {
			throw new EdenException("An error occurred while retrieving the paste data: " + ex.getMessage());
		}
	}

}
