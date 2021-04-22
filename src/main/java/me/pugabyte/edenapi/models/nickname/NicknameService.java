package me.pugabyte.edenapi.models.nickname;

import me.pugabyte.edenapi.persistence.MongoService;
import me.pugabyte.edenapi.persistence.annotations.PlayerClass;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(Nickname.class)
public class NicknameService extends MongoService<Nickname> {
	private final static Map<UUID, Nickname> cache = new HashMap<>();

	public Map<UUID, Nickname> getCache() {
		return cache;
	}

	public Nickname getFromNickname(String nickname) {
		Nickname data = database.createQuery(Nickname.class).filter("nickname", sanitize(nickname)).find().tryNext();
		cache(data);
		return data;
	}

	public Nickname getFromQueueId(String queueId) {
		Nickname data = database.createQuery(Nickname.class).filter("nicknameHistory.nicknameQueueId", sanitize(queueId)).find().tryNext();
		cache(data);
		return data;
	}

}
