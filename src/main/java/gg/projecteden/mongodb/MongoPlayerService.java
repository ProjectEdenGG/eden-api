package gg.projecteden.mongodb;

import gg.projecteden.exceptions.postconfigured.PlayerNotFoundException;
import gg.projecteden.interfaces.PlayerOwnedObject;
import gg.projecteden.models.nerd.Nerd;
import gg.projecteden.models.nerd.NerdService;
import gg.projecteden.utils.StringUtils;

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

	private static final Function<UUID, Boolean> isV4 = StringUtils::isV4Uuid;
	private static final Function<UUID, Boolean> is0 = StringUtils::isUUID0;
	private static final Function<UUID, Boolean> isApp = StringUtils::isAppUuid;

	private boolean isUuidValid(T object) {
		final UUID uuid = object.getUuid();
		return isV4.apply(uuid) || is0.apply(uuid) || isApp.apply(uuid);
	}

}