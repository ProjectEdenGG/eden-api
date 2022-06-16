package gg.projecteden.api.mongodb.models.nerd;

import dev.morphia.query.Query;
import gg.projecteden.api.common.exceptions.EdenException;
import gg.projecteden.api.common.utils.Utils;
import gg.projecteden.api.mongodb.MongoPlayerService;
import gg.projecteden.api.mongodb.annotations.ObjectClass;
import gg.projecteden.api.mongodb.models.hours.HoursService;
import gg.projecteden.api.mongodb.models.nickname.Nickname;
import gg.projecteden.api.mongodb.models.nickname.NicknameService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ObjectClass(Nerd.class)
public class NerdService extends MongoPlayerService<Nerd> {
	private final static Map<UUID, Nerd> cache = new HashMap<>();

	public Map<UUID, Nerd> getCache() {
		return cache;
	}

	public List<Nerd> find(String partialName) {
		Query<Nerd> query = database.createQuery(Nerd.class);
		query.and(query.criteria("pastNames").containsIgnoreCase(partialName));
		if (query.count() > 50)
			throw new EdenException("Too many name matches for &e" + partialName + " &c(" + query.count() + ")");

		Map<Nerd, Integer> hoursMap = new HashMap<>() {{
			HoursService service = new HoursService();
			for (Nerd nerd : query.find().toList())
				put(nerd, service.get(nerd.getUuid()).getTotal());
		}};

		return new ArrayList<>(Utils.sortByValueReverse(hoursMap).keySet());
	}

	public List<Nerd> getNerdsWithBirthdays() {
		Query<Nerd> query = database.createQuery(Nerd.class);
		query.and(query.criteria("birthday").notEqual(null));
		return query.find().toList();
	}

	public Nerd findExact(String name) {
		Query<Nerd> query = database.createQuery(Nerd.class);
		query.and(query.criteria("name").equalIgnoreCase(name));
		Nerd nerd = query.find().tryNext();

		if (nerd == null) {
			Nickname fromNickname = new NicknameService().getFromNickname(name);
			if (fromNickname != null)
				nerd = fromNickname.getNerd();
		}

		return nerd;
	}

	public Nerd getFromAlias(String alias) {
		return database.createQuery(Nerd.class).filter("aliases", alias).find().tryNext();
	}

}
