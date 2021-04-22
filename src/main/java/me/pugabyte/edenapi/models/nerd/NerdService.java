package me.pugabyte.edenapi.models.nerd;

import dev.morphia.query.Query;
import me.pugabyte.edenapi.models.hours.HoursService;
import me.pugabyte.edenapi.persistence.MongoService;
import me.pugabyte.edenapi.persistence.annotations.PlayerClass;
import me.pugabyte.edenapi.exceptions.EdenException;
import me.pugabyte.edenapi.utils.Utils;

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

		Map<Nerd, Integer> hoursMap = new HashMap<Nerd, Integer>() {{
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

	public Nerd getFromAlias(String alias) {
		return database.createQuery(Nerd.class).filter("aliases", sanitize(alias)).find().tryNext();
	}

}
