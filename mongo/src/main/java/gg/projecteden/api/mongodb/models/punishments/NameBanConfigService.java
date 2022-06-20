package gg.projecteden.api.mongodb.models.punishments;

import gg.projecteden.api.mongodb.MongoPlayerService;
import gg.projecteden.api.mongodb.annotations.ObjectClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static gg.projecteden.api.common.utils.UUIDUtils.UUID0;

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
