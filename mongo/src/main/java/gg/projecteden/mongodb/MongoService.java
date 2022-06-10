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
import gg.projecteden.EdenAPI;
import gg.projecteden.exceptions.EdenException;
import gg.projecteden.interfaces.DatabaseObject;
import gg.projecteden.interfaces.HasUniqueId;
import gg.projecteden.mongodb.annotations.ObjectClass;
import gg.projecteden.utils.Log;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.mongodb.MongoClient.getDefaultCodecRegistry;
import static gg.projecteden.utils.UUIDUtils.UUID0;
import static gg.projecteden.utils.Utils.subTypesOf;

public abstract class MongoService<T extends DatabaseObject> {
	protected static Datastore database;
	protected static String _id = "_id";

	@Getter
	private static final Set<Class<? extends MongoService>> services = subTypesOf(MongoService.class, MongoService.class.getPackageName() + ".models");
	@Getter
	private static final Map<Class<? extends DatabaseObject>, Class<? extends MongoService>> objectToServiceMap = new HashMap<>();
	@Getter
	private static final Map<Class<? extends MongoService>, Class<? extends DatabaseObject>> serviceToObjectMap = new HashMap<>();

	public static void loadServices() {
		loadServices(Collections.emptySet());
	}

	public static void loadServices(String... packages) {
		loadServices(subTypesOf(MongoService.class, packages));
	}

	public static void loadServices(Set<Class<? extends MongoService>> newServices) {
		services.addAll(newServices);
		for (Class<? extends MongoService> service : services) {
			if (Modifier.isAbstract(service.getModifiers()))
				continue;

			ObjectClass annotation = service.getAnnotation(ObjectClass.class);
			if (annotation == null) {
				Log.warn(service.getSimpleName() + " does not have @" + ObjectClass.class.getSimpleName() + " annotation");
				continue;
			}

			objectToServiceMap.put(annotation.value(), service);
			serviceToObjectMap.put(service, annotation.value());
		}
	}

	protected Class<T> getObjectClass() {
		ObjectClass annotation = getClass().getAnnotation(ObjectClass.class);
		return annotation == null ? null : (Class<T>) annotation.value();
	}

	public static Class<? extends DatabaseObject> ofService(MongoService mongoService) {
		return ofService(mongoService.getClass());
	}

	public static Class<? extends DatabaseObject> ofService(Class<? extends MongoService> mongoService) {
		return serviceToObjectMap.get(mongoService);
	}

	public static Class<? extends MongoService> ofObject(DatabaseObject object) {
		return ofObject(object.getClass());
	}

	public static Class<? extends MongoService> ofObject(Class<? extends DatabaseObject> object) {
		return objectToServiceMap.get(object);
	}

	static {
		database = MongoConnector.connect();
	}

	public static DBObject serialize(Object object) {
		return database.getMapper().toDBObject(object);
	}

	@SneakyThrows
	public static <C> C deserialize(DBObject dbObject) {
		final String className = (String) dbObject.get("className");
		try {
			final Class<C> clazz = (Class<C>) Class.forName(className);
			final EntityCache entityCache = database.getMapper().createEntityCache();
			return database.getMapper().fromDBObject(database, clazz, dbObject, entityCache);
		} catch (ClassNotFoundException ex) {
			Log.warn("Could not find class " + className);
			return null;
		}
	}

	public MongoCollection<Document> getCollection() {
		return database.getDatabase().getCollection(getObjectClass().getAnnotation(Entity.class).value());
	}

	public abstract Map<UUID, T> getCache();

	public void clearCache() {
		getCache().clear();
	}

	public Collection<T> cacheAll() {
		database.createQuery(getObjectClass()).find().forEachRemaining(this::cache);
		return getCache().values();
	}

	public void cache(T object) {
		if (object != null)
			getCache().putIfAbsent(object.getUuid(), object);
	}

	public boolean isCached(T object) {
		return getCache().containsKey(object.getUuid());
	}

	public void add(T object) {
		cache(object);
		save(object);
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

		for (T object : new ArrayList<>(getCache().values()))
			executor.submit(() -> saveSync(object));
	}

	private static final JsonWriterSettings jsonWriterSettings = JsonWriterSettings.builder().indent(true).build();

	public String asPrettyJson(UUID uuid) {
		final Document document = getCollection().find(new BasicDBObject(Map.of(_id, uuid.toString()))).first();
		if (document == null)
			throw new EdenException("Could not find matching document");

		return document.toBsonDocument(BsonDocument.class, getDefaultCodecRegistry()).toJson(jsonWriterSettings);
	}

	public T get(String name) {
		return get(UUID.fromString(name));
	}

	public T get(HasUniqueId object) {
		return get(object.getUniqueId());
	}

	@NotNull
	public T get(UUID uuid) {
//		if (isEnableCache())
		return getCache(uuid);
//		else
//			return getNoCache(uuid);
	}

	public T get0() {
		return get(UUID0);
	}

	public T getApp() {
		return get(EdenAPI.get().getAppUuid());
	}

	public void edit(String uuid, Consumer<T> consumer) {
		edit(get(uuid), consumer);
	}

	public void edit(HasUniqueId uuid, Consumer<T> consumer) {
		edit(get(uuid), consumer);
	}

	public void edit(UUID uuid, Consumer<T> consumer) {
		edit(get(uuid), consumer);
	}

	public void edit(T object, Consumer<T> consumer) {
		consumer.accept(object);
		save(object);
	}

	public void edit0(Consumer<T> consumer) {
		edit(get0(), consumer);
	}

	public void editApp(Consumer<T> consumer) {
		edit(getApp(), consumer);
	}

	public void save(T object) {
		checkType(object);
		saveSync(object);
	}

	private void checkType(T object) {
		if (getObjectClass() == null) return;
		if (!object.getClass().isAssignableFrom(getObjectClass()))
			throw new EdenException(this.getClass().getSimpleName() + " received wrong class type, expected "
					+ getObjectClass().getSimpleName() + ", found " + object.getClass().getSimpleName());
	}

	public void delete(T object) {
		checkType(object);
		deleteSync(object);
	}

	public void deleteAll() {
		deleteAllSync();
	}

	@NotNull
	protected T getCache(UUID uuid) {
		Objects.requireNonNull(getObjectClass(), "You must provide an owning class or override get(UUID)");
		if (getCache().containsKey(uuid) && getCache().get(uuid) == null)
			getCache().remove(uuid);
		return getCache().computeIfAbsent(uuid, $ -> getNoCache(uuid));
	}

	protected T getNoCache(UUID uuid) {
		T object = database.createQuery(getObjectClass()).field(_id).equal(uuid).first();
		if (object == null)
			object = createObject(uuid);
		if (object == null)
			Log.log("New instance of " + getObjectClass().getSimpleName() + " is null");
		return object;
	}

	protected T createObject(UUID uuid) {
		try {
			Constructor<? extends DatabaseObject> constructor = getObjectClass().getDeclaredConstructor(UUID.class);
			constructor.setAccessible(true);
			return (T) constructor.newInstance(uuid);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ex) {
			ex.printStackTrace();
			throw new EdenException(this.getClass().getSimpleName() + " does not have a UUID constructor (missing @NonNull on UUID or @RequiredArgsConstructor on class?)");
		}
	}

	public List<T> getPage(int page, int amount) {
		return database.createQuery(getObjectClass()).offset((page - 1) * amount).limit(amount).find().toList();
	}

	public List<T> getAll() {
		return database.createQuery(getObjectClass()).find().toList();
	}

	public List<T> getAllLimit(int limit) {
		return database.createQuery(getObjectClass()).limit(limit).find().toList();
	}

	public List<T> getAllSortedBy(Sort... sorts) {
		return database.createQuery(getObjectClass())
				.order(sorts)
				.find().toList();
	}

	public List<T> getAllSortedByLimit(int limit, Sort... sorts) {
		return database.createQuery(getObjectClass())
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
		beforeDelete(object);

		getCache().remove(object.getUuid());
		database.delete(object);
		getCache().remove(object.getUuid());
		object = null;
	}

	public void deleteAllSync() {
		database.getCollection(getObjectClass()).drop();
		clearCache();
	}

	@NotNull
	protected <U> List<U> map(AggregateIterable<Document> documents, Class<U> clazz) {
		return new ArrayList<>() {{
			for (Document purchase : documents)
				add(database.getMapper().fromDBObject(database, clazz, new BasicDBObject(purchase), null));
		}};
	}

}
