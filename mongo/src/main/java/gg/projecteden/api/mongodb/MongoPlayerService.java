package gg.projecteden.api.mongodb;

import gg.projecteden.api.common.exceptions.postconfigured.PlayerNotFoundException;
import gg.projecteden.api.common.utils.UUIDUtils;
import gg.projecteden.api.mongodb.interfaces.PlayerOwnedObject;
import gg.projecteden.api.mongodb.models.nerd.Nerd;
import gg.projecteden.api.mongodb.models.nerd.NerdService;

import java.util.UUID;
import java.util.function.Function;

public abstract class MongoPlayerService<T extends PlayerOwnedObject> extends MongoService<T> {

	public T get(String name) {
		Nerd nerd = new NerdService().findExact(name);
		if (nerd == null)
			throw new PlayerNotFoundException(name);
		return get(nerd);
	}

	public void saveSync(T object) {
		if (!isUuidValid(object))
			return;

		super.saveSync(object);
	}

	public void deleteSync(T object) {
		if (!isUuidValid(object))
			return;

		super.deleteSync(object);
	}

	private static final Function<UUID, Boolean> isV4 = UUIDUtils::isV4Uuid;
	private static final Function<UUID, Boolean> is0 = UUIDUtils::isUUID0;
	private static final Function<UUID, Boolean> isApp = UUIDUtils::isAppUuid;

	private boolean isUuidValid(T object) {
		final UUID uuid = object.getUuid();
		return isV4.apply(uuid) || is0.apply(uuid) || isApp.apply(uuid);
	}

}
