package gg.projecteden.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionUtils {

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

		superclasses.add(clazz);
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

	@SuppressWarnings("unchecked")
	public static <T> Set<Class<? extends T>> typesAnnotatedWith(Class<? extends Annotation> annotation, String... packages) {
		try (var scan = scanPackages(packages).scan()) {
			return scan.getClassesWithAnnotation(annotation).stream()
					.map(ClassInfo::loadClass)
					.map(clazz -> (Class<? extends T>) clazz)
					.collect(Collectors.toSet());
		}
	}

	public static Set<Method> methodsAnnotatedWith(Class<?> clazz, Class<? extends Annotation> annotation) {
		final HashSet<Method> methods = new HashSet<>() {{
			for (Method method : getAllMethods(clazz))
				if (method.getAnnotation(annotation) != null)
					add(method);
		}};
		if (clazz.getSimpleName().contains("Nexus"))
			System.out.println("Methods annotated with " + annotation.getSimpleName() + ": " + methods.size());
		return methods;
	}

	private static ClassGraph scanPackages(String... packages) {
		return new ClassGraph()
				.acceptPackages(packages)
				.enableClassInfo()
				.enableAnnotationInfo()
				.initializeLoadedClasses();
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<Class<? extends T>> getClasses(String[] packages, Predicate<ClassInfo> filter) {
		try (var scan = scanPackages(packages).scan()) {
			return scan.getAllClasses().stream()
					.filter(filter)
					.map(ClassInfo::loadClass)
					.map(clazz -> (Class<? extends T>) clazz)
					.collect(Collectors.toSet());
		}
	}

	private static Set<Method> getAllMethods(Class<?> clazz) {
		return new HashSet<>(new HashMap<String, Method>() {{
			final List<Class<?>> superclasses = Utils.reverse(superclassesOf(clazz));
			if (clazz.getSimpleName().contains("Nexus"))
				System.out.println("Superclasses: " + superclasses.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));

			for (Class<?> clazz : superclasses)
				for (Method method : clazz.getMethods())
					put(getMethodKey(method), method);

			if (clazz.getSimpleName().contains("Nexus"))
				System.out.println("Methods: " + size());
		}}.values());
	}

	@NotNull
	private static String getMethodKey(Method method) {
		final String params = Arrays.stream(method.getParameters())
				.map(parameter -> parameter.getType().getSimpleName())
				.collect(Collectors.joining(","));

		return "%s(%s)".formatted(method.getName(), params);
	}

}
