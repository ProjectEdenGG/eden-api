package gg.projecteden.utils;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

// lexi TODO: javadocs
public class RandomUtils {
	protected RandomUtils() {
		throw new IllegalStateException("Cannot instantiate utility class");
	}

	@Getter
	protected static final Random random = new Random();

	public static boolean chanceOf(int chance) {
		return chanceOf((double) chance);
	}

	public static boolean chanceOf(double chance) {
		if(chance <= 0.0)
			return false;

		return randomDouble(0, 100) <= chance;
	}

	public static int randomInt(int max) {
		return randomInt(0, max);
	}

	public static int randomInt(int min, int max) throws IllegalArgumentException {
		if (min == max) return min;
		if (min > max) throw new IllegalArgumentException("Min cannot be greater than max!");
		return min + random.nextInt(max-min+1);
	}

	public static double randomDouble() {
		return random.nextDouble();
	}

	public static double randomDouble(double max) {
		return randomDouble(0, max);
	}

	public static double randomDouble(double min, double max) throws IllegalArgumentException {
		if (min == max) return min;
		if (min > max) throw new IllegalArgumentException("Min cannot be greater than max!");
		return min + (max - min) * random.nextDouble();
	}

	public static String randomAlphanumeric() {
		return randomElement(Utils.ALPHANUMERICS.split(""));
	}

	@Contract("null -> null")
	public static <T> T randomElement(T @Nullable... list) {
		if (list == null || list.length == 0)
			return null;
		return list[random.nextInt(list.length)];
	}

	@Contract("null -> null")
	public static <T> T randomElement(@Nullable Collection<@Nullable T> list) {
		if (Nullables.isNullOrEmpty(list)) return null;
		int getIndex = random.nextInt(list.size());
		int currentIndex = 0;
		for (T item : list) {
			if (currentIndex++ == getIndex)
				return item;
		}
		throw new IllegalStateException("Collection was altered during iteration");
	}

	public static <T> T randomElement(@NotNull Class<? extends T> enumClass) {
		return randomElement(enumClass.getEnumConstants());
	}

	@Contract("null -> null")
	private static <T> T randomElement(@Nullable List<@Nullable T> list) {
		if (Nullables.isNullOrEmpty(list)) return null;
		return list.get(random.nextInt(list.size()));
	}

	public static double randomAngle() {
		return random.nextDouble() * 2 * Math.PI;
	}

	public static <E> E getWeightedRandom(@NotNull Map<E, Double> weights) {
		return Utils.getMin(weights.keySet(), element -> -Math.log(RandomUtils.getRandom().nextDouble()) / weights.get(element)).getObject();
	}

}
