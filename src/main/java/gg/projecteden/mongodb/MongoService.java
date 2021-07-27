package gg.projecteden.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.cache.EntityCache;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateException;
import gg.projecteden.exceptions.EdenException;
import gg.projecteden.exceptions.postconfigured.PlayerNotFoundException;
import gg.projecteden.interfaces.PlayerOwnedObject;
import gg.projecteden.models.nerd.Nerd;
import gg.projecteden.models.nerd.NerdService;
import gg.projecteden.mongodb.annotations.PlayerClass;
import gg.projecteden.utils.Log;
import gg.projecteden.utils.StringUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import me.lexikiq.HasUniqueId;
import org.apache.commons.lang3.Validate;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.mongodb.MongoClient.getDefaultCodecRegistry;

public abstract class MongoService<T extends PlayerOwnedObject> {
	protected static Datastore database;
	protected static String _id = "_id";

	@Getter
	private static final Set<Class<? extends MongoService>> services = new Reflections("eden.models").getSubTypesOf(MongoService.class);
	@Getter
	private static final Map<Class<? extends PlayerOwnedObject>, Class<? extends MongoService>> objectToServiceMap = new HashMap<>();
	@Getter
	private static final Map<Class<? extends MongoService>, Class<? extends PlayerOwnedObject>> serviceToObjectMap = new HashMap<>();

	static {
		loadServices();
	}

	public static void loadServices() {
		loadServices(Collections.emptySet());
	}

	public static void loadServices(String path) {
		loadServices(new Reflections(path));
	}

	public static void loadServices(Reflections reflections) {
		loadServices(reflections.getSubTypesOf(MongoService.class));
	}

	public static void loadServices(Set<Class<? extends MongoService>> newServices) {
		services.addAll(newServices);
		for (Class<? extends MongoService> service : services) {
			if (Modifier.isAbstract(service.getModifiers()))
				continue;

			PlayerClass annotation = service.getAnnotation(PlayerClass.class);
			if (annotation == null) {
				Log.warn(service.getSimpleName() + " does not have @PlayerClass annotation");
				continue;
			}

			objectToServiceMap.put(annotation.value(), service);
			serviceToObjectMap.put(service, annotation.value());
		}
	}

	protected Class<T> getPlayerClass() {
		PlayerClass annotation = getClass().getAnnotation(PlayerClass.class);
		return annotation == null ? null : (Class<T>) annotation.value();
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
		database = MongoConnector.connect();
	}

	public static DBObject serialize(Object object) {
		return database.getMapper().toDBObject(object);
	}

	@SneakyThrows
	public static <C> C deserialize(DBObject dbObject) {
		final Class<C> className = (Class<C>) Class.forName((String) dbObject.get("className"));
		final EntityCache entityCache = database.getMapper().createEntityCache();
		return database.getMapper().fromDBObject(database, className, dbObject, entityCache);
	}

	public MongoCollection<Document> getCollection() {
		return database.getDatabase().getCollection(getPlayerClass().getAnnotation(Entity.class).value());
	}

	public abstract Map<UUID, T> getCache();

	public void clearCache() {
		getCache().clear();
	}

	public void cache(T object) {
		if (object != null)
			getCache().put(object.getUuid(), object);
	}

	public boolean isCached(T object) {
		return getCache().containsKey(object.getUuid());
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

		for (T player : new ArrayList<>(getCache().values()))
			executor.submit(() -> saveSync(player));
	}

	private static final JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder().indent(true).build();

	public String asPrettyJson(UUID uuid) {
		final Document document = getCollection().find(new BasicDBObject(Map.of(_id, uuid.toString()))).first();
		if (document == null)
			throw new EdenException("Could not find matching document");

		return document.toBsonDocument(BsonDocument.class, getDefaultCodecRegistry()).toJson(jsonWriterSettings);
	}

	public T get(String name) {
		Nerd nerd = new NerdService().findExact(name);
		if (nerd == null)
			throw new PlayerNotFoundException(name);
		return get(nerd);
	}

	public T get(HasUniqueId player) {
		return get(player.getUniqueId());
	}

	@NotNull
	public T get(UUID uuid) {
//		if (isEnableCache())
		return getCache(uuid);
//		else
//			return getNoCache(uuid);
	}

	public T get0() {
		return get(StringUtils.getUUID0());
	}

	public void edit(String player, Consumer<T> consumer) {
		edit(get(player), consumer);

	}

	public void edit(HasUniqueId player, Consumer<T> consumer) {
		edit(get(player), consumer);
	}

	public void edit(UUID player, Consumer<T> consumer) {
		edit(get(player), consumer);
	}

	public void edit(T object, Consumer<T> consumer) {
		consumer.accept(object);
		save(object);
	}

	public void edit0(Consumer<T> consumer) {
		edit(get0(), consumer);
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

	protected String sanitize(String input) {
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
		T object = database.createQuery(getPlayerClass()).field(_id).equal(uuid).first();
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
			throw new EdenException(this.getClass().getSimpleName() + " does not have a UUID constructor (missing @NonNull on UUID or @RequiredArgsConstructor on class?)");
		}
	}

	public List<T> getPage(int page, int amount) {
		return database.createQuery(getPlayerClass()).offset((page - 1) * amount).limit(amount).find().toList();
	}

	public List<T> getAll() {
		return database.createQuery(getPlayerClass()).find().toList();
	}

	public List<T> getAllLimit(int limit) {
		return database.createQuery(getPlayerClass()).limit(limit).find().toList();
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

	protected boolean deleteIf(T object) {
		return false;
	}

	protected void beforeSave(T object) {
	}

	protected void beforeDelete(T object) {
	}

	public void saveSync(T object) {
		if (!StringUtils.isV4Uuid(object.getUuid()) && !object.getUuid().equals(StringUtils.getUUID0()))
			return;

		beforeSave(object);

		if (deleteIf(object)) {
			deleteSync(object);
			return;
		}

		saveSyncReal(object);
	}

	protected void saveSyncReal(T object) {
		try {
			database.merge(object);
		} catch (UpdateException doesntExistYet) {
			try {
				database.save(object);
			} catch (Exception ex2) {
				handleSaveException(object, ex2, "saving");
			}
		} catch (Exception ex3) {
			handleSaveException(object, ex3, "updating");
		}
	}

	protected void handleSaveException(T object, Exception ex, String type) {
		String toString = object.toString();
		String extra = toString.length() >= Short.MAX_VALUE ? "" : ": " + toString;
		Log.warn("Error " + type + " " + object.getClass().getSimpleName() + extra);
		ex.printStackTrace();
	}

	public void deleteSync(T object) {
		if (!StringUtils.isV4Uuid(object.getUuid()) && !object.getUuid().equals(StringUtils.getUUID0()))
			return;

		beforeDelete(object);

		getCache().remove(object.getUuid());
		database.delete(object);
		getCache().remove(object.getUuid());
		object = null;
	}

	public void deleteAllSync() {
		database.getCollection(getPlayerClass()).drop();
		clearCache();
	}

	@NotNull
	protected <U> List<U> map(AggregateIterable<Document> documents, Class<U> clazz) {
		return new ArrayList<>() {{
			for (Document purchase : documents)
				add(database.getMapper().fromDBObject(database, clazz, new BasicDBObject(purchase), null));
		}};
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
