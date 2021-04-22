package me.pugabyte.edenapi.mongodb;

import dev.morphia.Datastore;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateException;
import lombok.Getter;
import me.pugabyte.edenapi.EdenAPI;
import me.pugabyte.edenapi.exceptions.EdenException;
import me.pugabyte.edenapi.models.PlayerOwnedObject;
import me.pugabyte.edenapi.models.nerd.Nerd;
import me.pugabyte.edenapi.mongodb.annotations.PlayerClass;
import me.pugabyte.edenapi.utils.Log;
import me.pugabyte.edenapi.utils.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static me.pugabyte.edenapi.utils.StringUtils.isV4Uuid;

public abstract class MongoService<T extends PlayerOwnedObject> {
	protected static Datastore database;
	protected static String _id = "_id";

	@Getter
	private static final Set<Class<? extends MongoService>> services = new Reflections(MongoService.class.getPackage().getName()).getSubTypesOf(MongoService.class);
	@Getter
	private static final Map<Class<? extends PlayerOwnedObject>, Class<? extends MongoService>> objectToServiceMap = new HashMap<>();
	@Getter
	private static final Map<Class<? extends MongoService>, Class<? extends PlayerOwnedObject>> serviceToObjectMap = new HashMap<>();

	static {
		for (Class<? extends MongoService> service : services) {
			PlayerClass annotation = service.getAnnotation(PlayerClass.class);
			if (annotation == null) {
				Log.warn(service.getSimpleName() + " does not have @PlayerClass annotation");
				continue;
			}

			objectToServiceMap.put(annotation.value(), service);
			serviceToObjectMap.put(service, annotation.value());
		}
	}

	public static Class<? extends PlayerOwnedObject> ofService(MongoService mongoService) {
		return ofService(mongoService.getClass());
	}

	public static Class<? extends PlayerOwnedObject> ofService(Class<? extends MongoService> mongoService) {
		return serviceToObjectMap.get(mongoService);
	}

	public static Class<? extends MongoService> ofObject(PlayerOwnedObject playerOwnedObject) {
		return ofObject(playerOwnedObject.getClass());
	}

	public static Class<? extends MongoService> ofObject(Class<? extends PlayerOwnedObject> playerOwnedObject) {
		return objectToServiceMap.get(playerOwnedObject);
	}

	static {
		database = MongoDBPersistence.getConnection(EdenAPI.api().getDatabaseConfig());
	}

	public abstract Map<UUID, T> getCache();

	public void clearCache() {
		getCache().clear();
	}

	public void cache(T object) {
		if (object != null)
			getCache().put(object.getUuid(), object);
	}

	public void saveCache() {
		saveCache(100);
	}

	public void saveCache(int threadCount) {
		saveCacheSync(threadCount);
	}

	public void saveCacheSync() {
		saveCacheSync(100);
	}

	public void saveCacheSync(int threadCount) {
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		for (T player : getCache().values())
			executor.submit(() -> saveSync(player));
	}

	public Class<T> getPlayerClass() {
		PlayerClass annotation = getClass().getAnnotation(PlayerClass.class);
		return annotation == null ? null : (Class<T>) annotation.value();
	}

	public T get(Nerd nerd) {
		return get(nerd.getUuid());
	}

	public T get(PlayerOwnedObject player) {
		return get(player.getUuid());
	}

	@NotNull
	public T get(UUID uuid) {
//		if (isEnableCache())
		return getCache(uuid);
//		else
//			return getNoCache(uuid);
	}

	public void save(T object) {
		checkType(object);
		saveSync(object);
	}

	private void checkType(T object) {
		if (getPlayerClass() == null) return;
		if (!object.getClass().isAssignableFrom(getPlayerClass()))
			throw new EdenException(this.getClass().getSimpleName() + " received wrong class type, expected "
					+ getPlayerClass().getSimpleName() + ", found " + object.getClass().getSimpleName());
	}

	public void delete(T object) {
		checkType(object);
		deleteSync(object);
	}

	public void deleteAll() {
		deleteAllSync();
	}

	public String sanitize(String input) {
		if (Pattern.compile("[\\w\\d\\s]+").matcher(input).matches())
			return input;
		throw new EdenException("Unsafe argument");
	}

	@NotNull
	protected T getCache(UUID uuid) {
		Validate.notNull(getPlayerClass(), "You must provide a player owned class or override get(UUID)");
		if (getCache().containsKey(uuid) && getCache().get(uuid) == null)
			getCache().remove(uuid);
		return getCache().computeIfAbsent(uuid, $ -> getNoCache(uuid));
	}

	protected T getNoCache(UUID uuid) {
		System.out.println(getPlayerClass().getSimpleName());
		System.out.println(_id);
		System.out.println(uuid);
		System.out.println(database.getDatabase().getName());
		T object = database.createQuery(getPlayerClass()).field(_id).equal(uuid).first();
		System.out.println("Object: " + null);
		if (object == null)
			object = createPlayerObject(uuid);
		if (object == null)
			Log.log("New instance of " + getPlayerClass().getSimpleName() + " is null");
		return object;
	}

	protected T createPlayerObject(UUID uuid) {
		try {
			Constructor<? extends PlayerOwnedObject> constructor = getPlayerClass().getDeclaredConstructor(UUID.class);
			constructor.setAccessible(true);
			return (T) constructor.newInstance(uuid);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
			ex.printStackTrace();
			throw new EdenException(this.getClass().getSimpleName() + " not implemented correctly");
		}
	}

	public List<T> getAll() {
		return database.createQuery(getPlayerClass()).find().toList();
	}

	public List<T> getAllSortedBy(Sort... sorts) {
		return database.createQuery(getPlayerClass())
				.order(sorts)
				.find().toList();
	}

	public List<T> getAllSortedByLimit(int limit, Sort... sorts) {
		return database.createQuery(getPlayerClass())
				.order(sorts)
				.limit(limit)
				.find().toList();
	}

	public void saveSync(T object) {
		if (!isV4Uuid(object.getUuid()) && !object.getUuid().equals(StringUtils.getUUID0()))
			return;

		try {
			database.merge(object);
		} catch (UpdateException doesntExistYet) {
			try {
				database.save(object);
			} catch (Exception ex2) {
				String toString = object.toString();
				Log.warn("Error saving " + object.getClass().getSimpleName() + (toString.length() >= Short.MAX_VALUE ? "" : ": " + toString));
				ex2.printStackTrace();
			}
		} catch (Exception ex3) {
			String toString = object.toString();
			Log.warn("Error updating " + object.getClass().getSimpleName() + (toString.length() >= Short.MAX_VALUE ? "" : ": " + toString));
			ex3.printStackTrace();
		}
	}

	public void deleteSync(T object) {
		if (!isV4Uuid(object.getUuid()) && !object.getUuid().equals(StringUtils.getUUID0()))
			return;

		database.delete(object);
		getCache().remove(object.getUuid());
	}

	public void deleteAllSync() {
		database.getCollection(getPlayerClass()).drop();
		clearCache();
	}

	/*
	public void log(String name) {
		try {
			try {
				throw new BNException("Stacktrace");
			} catch (BNException ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				Nexus.fileLogSync("pugmas-db-debug", "[Primary thread: " + Bukkit.isPrimaryThread() + "] MongoDB Pugmas20 " + name + "\n" + sw.toString() + "\n");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	*/
}
