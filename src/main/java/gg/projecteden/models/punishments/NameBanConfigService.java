package gg.projecteden.models.punishments;

import gg.projecteden.mongodb.MongoPlayerService;
import gg.projecteden.mongodb.annotations.ObjectClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gg.projecteden.utils.UUIDUtils.UUID0;

@ObjectClass(NameBanConfig.class)
public class NameBanConfigService extends MongoPlayerService<NameBanConfig> {
	private final static Map<UUID, NameBanConfig> cache = new HashMap<>();

	public Map<UUID, NameBanConfig> getCache() {
		return cache;
	}

	public NameBanConfig get() {
		return get(UUID0);
	}

}
