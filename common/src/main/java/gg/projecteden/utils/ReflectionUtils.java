package gg.projecteden.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionUtils {
	private static final Map<String, ScanResult> PACKAGE_SCAN_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, ScanResult> CLASS_SCAN_CACHE = new ConcurrentHashMap<>();

	/**
	 * Returns a list of superclasses, including the provided class
	 *
	 * @param clazz subclass
	 * @return superclasses
	 */
	public static <T> List<Class<? extends T>> superclassesOf(Class<? extends T> clazz) {
		List<Class<? extends T>> superclasses = new ArrayList<>();
		while (clazz.getSuperclass() != Object.class) {
			superclasses.add(clazz);
			clazz = (Class<? extends T>) clazz.getSuperclass();
		}

		return superclasses;
	}

	public static <T> Set<Class<? extends T>> subTypesOf(Class<T> superclass, String... packages) {
		return getClasses(packages, subclass -> {
			if (superclass.isInterface())
				return subclass.implementsInterface(superclass);
			else
				return subclass.extendsSuperclass(superclass);
		});
	}

	public static <T> Set<Class<? extends T>> typesAnnotatedWith(Class<? extends Annotation> annotation, String... packages) {
		return getClasses(packages, clazz -> clazz.hasAnnotation(annotation));
	}

	public static Set<Method> methodsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
		return getMethods(clazz, method -> method.hasAnnotation(annotation));
	}

	private static ScanResult scanPackages(String... packages) {
		return PACKAGE_SCAN_CACHE.computeIfAbsent(getCacheKey(packages), $ -> new ClassGraph()
				.acceptPackages(packages)
				.enableAllInfo()
				.initializeLoadedClasses()
				.scan());
	}

	private static ScanResult scanClasses(String... classes) {
		return PACKAGE_SCAN_CACHE.computeIfAbsent(getCacheKey(classes), $ -> new ClassGraph()
				.acceptClasses(classes)
				.enableAllInfo()
				.initializeLoadedClasses()
				.scan());
	}

	@NotNull
	private static String getCacheKey(String[] packages) {
		return Arrays.stream(packages).sorted().collect(Collectors.joining(":"));
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<Class<? extends T>> getClasses(String[] packages, Predicate<ClassInfo> filter) {
		try (ScanResult scan = scanPackages(packages)) {
			return scan.getAllClasses().stream()
					.filter(filter)
					.map(ClassInfo::loadClass)
					.map(clazz -> (Class<? extends T>) clazz)
					.collect(Collectors.toSet());
		}
	}

	private static <T> Set<Method> getMethods(Class<?> clazz, Predicate<MethodInfo> filter) {
		try (ScanResult scan = scanClasses(clazz.getName())) {
			return scan.getClassInfo(clazz.getName()).getMethodInfo().stream()
					.filter(filter)
					.map(MethodInfo::loadClassAndGetMethod)
					.collect(Collectors.toSet());
		}
	}

	private static <T> Set<Method> getAllMethods(Class<?> clazz, Predicate<MethodInfo> filter) {
		List<String> classes = superclassesOf(clazz).stream().map(Class::getName).toList();
		try (ScanResult scan = scanClasses(classes.toArray(String[]::new))) {
			return scan.getClassInfo(clazz.getName()).getMethodInfo().stream()
					.filter(filter)
					.map(MethodInfo::loadClassAndGetMethod)
					.collect(Collectors.toSet());
		}
	}

}
