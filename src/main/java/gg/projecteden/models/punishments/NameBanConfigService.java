package gg.projecteden.models.punishments;

import gg.projecteden.mongodb.MongoService;
import gg.projecteden.mongodb.annotations.PlayerClass;
import gg.projecteden.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(NameBanConfig.class)
public class NameBanConfigService extends MongoService<NameBanConfig> {
	private final static Map<UUID, NameBanConfig> cache = new HashMap<>();

	public Map<UUID, NameBanConfig> getCache() {
		return cache;
	}

	public NameBanConfig get() {
		return get(StringUtils.getUUID0());
	}

}
