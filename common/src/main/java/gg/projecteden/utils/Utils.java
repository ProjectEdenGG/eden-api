package gg.projecteden.utils;

import gg.projecteden.annotations.Disabled;
import gg.projecteden.annotations.Environments;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

	public static <K extends Comparable<? super K>, V> LinkedHashMap<K, V> sortByKey(Map<K, V> map) {
		return collect(map.entrySet().stream().sorted(Entry.comparingByKey()));
	}

	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValue(Map<K, V> map) {
		return collect(map.entrySet().stream().sorted(Entry.comparingByValue()));
	}

	public static <K extends Comparable<? super K>, V> LinkedHashMap<K, V> sortByKeyReverse(Map<K, V> map) {
		return reverse(sortByKey(map));
	}

	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValueReverse(Map<K, V> map) {
		return reverse(sortByValue(map));
	}

	public static <K, V> LinkedHashMap<K, V> collect(Stream<Entry<K, V>> stream) {
		return stream.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static <K, V> LinkedHashMap<K, V> reverse(LinkedHashMap<K, V> sorted) {
		LinkedHashMap<K, V> reverse = new LinkedHashMap<>();
		List<K> keys = new ArrayList<>(sorted.keySet());
		Collections.reverse(keys);
		keys.forEach(key -> reverse.put(key, sorted.get(key)));
		return reverse;
	}

	@Data
	@AllArgsConstructor
	public static class MinMaxResult<T> {
		private final T object;
		private final Number value;

		public int getInteger() {
			return value.intValue();
		}
		public double getDouble() {
			return value.doubleValue();
		}
		public float getFloat() {
			return value.floatValue();
		}
		public byte getByte() {
			return value.byteValue();
		}
		public short getShort() {
			return value.shortValue();
		}
		public long getLong() {
			return value.longValue();
		}
	}

	@AllArgsConstructor
	public enum ArithmeticOperator {
		ADD((n1, n2) -> n1.doubleValue() + n2.doubleValue()),
		SUBTRACT((n1, n2) -> n1.doubleValue() - n2.doubleValue()),
		MULTIPLY((n1, n2) -> n1.doubleValue() * n2.doubleValue()),
		DIVIDE((n1, n2) -> n1.doubleValue() / n2.doubleValue()),
		POWER((n1, n2) -> Math.pow(n1.doubleValue(), n2.doubleValue()));

		private final BiFunction<Number, Number, Number> function;

		public Number run(Number number1, Number number2) {
			return function.apply(number1, number2);
		}
	}

	@AllArgsConstructor
	public enum ComparisonOperator {
		LESS_THAN((n1, n2) -> n1.doubleValue() < n2.doubleValue()),
		GREATER_THAN((n1, n2) -> n1.doubleValue() > n2.doubleValue()),
		LESS_THAN_OR_EQUAL_TO((n1, n2) -> n1.doubleValue() <= n2.doubleValue()),
		GREATER_THAN_OR_EQUAL_TO((n1, n2) -> n1.doubleValue() >= n2.doubleValue());

		private final BiPredicate<Number, Number> predicate;

		public boolean run(Number number1, Number number2) {
			return predicate.test(number1, number2);
		}
	}

	public static <T> MinMaxResult<T> getMax(Collection<T> things, Function<T, Number> getter) {
		return getMinMax(things, getter, ComparisonOperator.GREATER_THAN);
	}

	public static <T> MinMaxResult<T> getMin(Collection<T> things, Function<T, Number> getter) {
		return getMinMax(things, getter, ComparisonOperator.LESS_THAN);
	}

	private static <T> MinMaxResult<T> getMinMax(Collection<T> things, Function<T, Number> getter, ComparisonOperator operator) {
		Number number = operator == ComparisonOperator.LESS_THAN ? Double.MAX_VALUE : 0;
		T result = null;

		for (T thing : things) {
			Number value = getter.apply(thing);
			if (value == null)
				continue;

			if (operator.run(value.doubleValue(), number.doubleValue())) {
				number = value;
				result = thing;
			}
		}

		return new MinMaxResult<>(result, number);
	}

	public static Map<String, String> dump(Object object) {
		Map<String, String> output = new HashMap<>();
		List<Method> methods = new ArrayList<>() {{
			for (Class<?> superclass : ReflectionUtils.superclassesOf(object.getClass()))
				addAll(Arrays.asList(superclass.getDeclaredMethods()));
		}};

		for (Method method : methods) {
			if (method.getName().matches("^(get|is|has).*") && method.getParameterCount() == 0) {
				try {
					final Object invoke = method.invoke(object);
					output.put(method.getName(), invoke == null ? null : invoke.toString());
				} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

		return output;
	}

	public static LocalDateTime epochSecond(String timestamp) {
		// try catch for MinecraftServers.Biz giving timestamp instead of epoch second
		try {
			return epochSecond(Long.parseLong(timestamp));
		} catch (NumberFormatException ex) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxxx");
			return LocalDateTime.parse(timestamp, formatter);
		}
	}

	public static LocalDateTime epochSecond(long timestamp) {
		return epochMilli(String.valueOf(timestamp).length() == 13 ? timestamp : timestamp * 1000);
	}

	public static LocalDateTime epochMilli(long timestamp) {
		return Instant.ofEpochMilli(timestamp)
				.atZone(ZoneId.systemDefault())
				.toLocalDateTime();
	}

	public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
		return !(dateTime.isBefore(start) || dateTime.isAfter(end));
	}

	public static boolean isBetween(LocalDate dateTime, LocalDate start, LocalDate end) {
		return !(dateTime.isBefore(start) || dateTime.isAfter(end));
	}

	public static boolean canEnable(Class<?> clazz) {
		if (clazz.getSimpleName().startsWith("_"))
			return false;
		if (Modifier.isAbstract(clazz.getModifiers()))
			return false;
		if (Modifier.isInterface(clazz.getModifiers()))
			return false;
		if (clazz.getAnnotation(Disabled.class) != null)
			return false;
		if (clazz.getAnnotation(Environments.class) != null && !Env.applies(clazz.getAnnotation(Environments.class).value()))
			return false;

		return true;
	}

	public static <T> List<T> combine(Collection<T>... lists) {
		return new ArrayList<>() {{
			for (Collection<T> list : lists)
				addAll(list);
		}};
	}

	public static int getFirstIndexOf(Collection<?> collection, Object object) {
		Iterator<?> iterator = collection.iterator();
		int index = 0;
		while (iterator.hasNext()) {
			Object next = iterator.next();
			if (next == null)
				if (object == null)
					return index;
				else
					++index;
			else if (next.equals(object))
				return index;
			else
				++index;
		}

		return -1;
	}

	public static boolean attempt(int times, BooleanSupplier to) {
		int count = 0;
		while (++count <= times)
			if (to.getAsBoolean())
				return true;
		return false;
	}

	public static final String ALPHANUMERICS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static boolean isLong(String text) {
		try {
			Long.parseLong(text);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean isInt(String text) {
		try {
			Integer.parseInt(text);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public static boolean isDouble(String text) {
		try {
			Double.parseDouble(text);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@SneakyThrows
	public static String createSha1(String url) {
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		return createSha1(response.body());
	}

	@SneakyThrows
	public static String createSha1(ResponseBody body) {
		if (body != null)
			return SHAsum(body.bytes());
		return null;
	}

	public static String SHAsum(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		return byteArray2Hex(md.digest(bytes));
	}

	private static String byteArray2Hex(final byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash)
			formatter.format("%02x", b);
		return formatter.toString();
	}

	public static <T> T getDefaultPrimitiveValue(Class<T> clazz) {
		return (T) Array.get(Array.newInstance(clazz, 1), 0);
	}

	public static boolean isBoolean(Parameter parameter) {
		return parameter.getType() == Boolean.class || parameter.getType() == Boolean.TYPE;
	}

	/**
	 * Removes the first element from an iterable that passes the {@code predicate}.
	 * @param predicate the predicate which returns true when an element should be removed
	 * @param from collection to remove an object from
	 * @return the object that was removed or null
	 */
	@Contract(mutates = "param2")
	public static <T> T removeFirstIf(Predicate<T> predicate, Iterable<T> from) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(from, "from");

		Iterator<T> iterator = from.iterator();
		while (iterator.hasNext()) {
			T item = iterator.next();
			if (predicate.test(item)) {
				iterator.remove();
				return item;
			}
		}
		return null;
	}

	/**
	 * Removes any element from an iterable that passes the {@code predicate}.
	 * @param predicate the predicate which returns true when an element should be removed
	 * @param from collection to remove an object from
	 * @return whether an object was removed
	 */
	@Contract(mutates = "param2")
	public static <T> boolean removeIf(Predicate<T> predicate, Iterable<T> from) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(from, "from");

		boolean removed = false;
		Iterator<T> iterator = from.iterator();
		while (iterator.hasNext()) {
			T item = iterator.next();
			if (predicate.test(item)) {
				iterator.remove();
				removed = true;
			}
		}
		return removed;
	}

	/**
	 * Removes any element from an iterable that passes the {@code predicate}
	 * and applies it to {@code consumer}.
	 * @param predicate the predicate which returns true when an element should be removed
	 * @param consumer consumer to perform an action on removed elements
	 * @param from collection to remove an object from
	 * @return whether an object was removed
	 */
	@Contract(mutates = "param2")
	public static <T> boolean removeIf(Predicate<T> predicate, Consumer<T> consumer, Iterable<T> from) {
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(from, "from");

		boolean removed = false;
		Iterator<T> iterator = from.iterator();
		while (iterator.hasNext()) {
			T item = iterator.next();
			if (predicate.test(item)) {
				consumer.accept(item);
				iterator.remove();
				removed = true;
			}
		}
		return removed;
	}

	/**
	 * Removes any element from an iterable that passes {@link Objects#equals(Object, Object)}.
	 * @param item item to remove from the list
	 * @param from collection to remove the item from
	 * @return whether an object was removed
	 */
	public static <T> boolean removeAll(T item, Iterable<T> from) {
		return removeIf(object -> Objects.equals(object, item), from);
	}

	/**
	 * Removes any element from an iterable that passes {@link Objects#equals(Object, Object)}
	 * and applies it to {@code consumer}.
	 * @param item item to remove from the list
	 * @param consumer consumer to perform an action on removed elements
	 * @param from collection to remove the item from
	 * @return whether an object was removed
	 */
	public static <T> boolean removeAll(T item, Consumer<T> consumer, Iterable<T> from) {
		return removeIf(object -> Objects.equals(object, item), consumer, from);
	}

	public static <T> @NotNull List<T> mutableCopyOf(@Nullable List<T> list) {
		return new ArrayList<>(list == null ? Collections.emptyList() : list);
	}

	@SneakyThrows
	public static String bash(String command) {
		return bash(command, null);
	}

	@SneakyThrows
	public static String bash(String command, File directory) {
		InputStream result = Runtime.getRuntime().exec(command, null, directory).getInputStream();
		StringBuilder builder = new StringBuilder();
		new Scanner(result).forEachRemaining(string -> builder.append(string).append(" "));
		return builder.toString().trim();
	}

}
