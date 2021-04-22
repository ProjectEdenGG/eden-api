package edenapi.models.punishments;

import edenapi.mongodb.MongoService;
import edenapi.mongodb.annotations.PlayerClass;
import edenapi.utils.StringUtils;

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
