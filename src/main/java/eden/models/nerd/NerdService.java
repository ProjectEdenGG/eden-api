package eden.models.nerd;

import dev.morphia.query.Query;
import eden.exceptions.EdenException;
import eden.models.hours.HoursService;
import eden.models.nickname.Nickname;
import eden.models.nickname.NicknameService;
import eden.mongodb.MongoService;
import eden.mongodb.annotations.PlayerClass;
import eden.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PlayerClass(Nerd.class)
public class NerdService extends MongoService<Nerd> {
	private final static Map<UUID, Nerd> cache = new HashMap<>();

	public Map<UUID, Nerd> getCache() {
		return cache;
	}

	public List<Nerd> find(String partialName) {
		Query<Nerd> query = database.createQuery(Nerd.class);
		query.and(query.criteria("pastNames").containsIgnoreCase(sanitize(partialName)));
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
		return database.createQuery(Nerd.class).filter("aliases", sanitize(alias)).find().tryNext();
	}

}
