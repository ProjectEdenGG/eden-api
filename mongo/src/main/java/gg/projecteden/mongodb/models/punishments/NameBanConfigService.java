package gg.projecteden.mongodb.models.punishments;

import gg.projecteden.mongodb.MongoPlayerService;
import gg.projecteden.mongodb.annotations.ObjectClass;
import gg.projecteden.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ObjectClass(NameBanConfig.class)
public class NameBanConfigService extends MongoPlayerService<NameBanConfig> {
	private final static Map<UUID, NameBanConfig> cache = new HashMap<>();

	public Map<UUID, NameBanConfig> getCache() {
		return cache;
	}

	public NameBanConfig get() {
		return get(StringUtils.getUUID0());
	}

}
