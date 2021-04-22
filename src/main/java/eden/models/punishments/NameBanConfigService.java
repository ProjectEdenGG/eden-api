package eden.models.punishments;

import eden.mongodb.MongoService;
import eden.mongodb.annotations.PlayerClass;
import eden.utils.StringUtils;

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
