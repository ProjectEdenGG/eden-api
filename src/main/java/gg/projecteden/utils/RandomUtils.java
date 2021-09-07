package gg.projecteden.utils;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomUtils {
	@Getter
	protected static final Random random = new Random();

	public static boolean chanceOf(int chance) {
		return chanceOf((double) chance);
	}

	public static boolean chanceOf(double chance) {
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

	public static <T> T randomElement(Object... list) {
		return (T) randomElement(Arrays.asList(list));
	}

	public static <T> T randomElement(Collection<T> list) {
		return randomElement(new ArrayList<>(list));
	}

	public static <T> T randomElement(Class<? extends T> clazz) {
		return randomElement((Object[]) clazz.getEnumConstants());
	}

	private static <T> T randomElement(List<T> list) {
		if (Utils.isNullOrEmpty(list)) return null;
		return new ArrayList<>(list).get(random.nextInt(list.size()));
	}

	public static double randomAngle() {
		return random.nextDouble() * 2 * Math.PI;
	}

	public static <E> E getWeightedRandom(Map<E, Double> weights) {
		return Utils.getMin(weights.keySet(), element -> -Math.log(RandomUtils.getRandom().nextDouble()) / weights.get(element)).getObject();
	}

}
