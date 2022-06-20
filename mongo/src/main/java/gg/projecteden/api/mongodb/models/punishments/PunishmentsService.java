package gg.projecteden.api.mongodb.models.punishments;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import dev.morphia.annotations.Id;
import dev.morphia.query.Query;
import gg.projecteden.api.mongodb.MongoPlayerService;
import gg.projecteden.api.mongodb.annotations.ObjectClass;
import gg.projecteden.api.mongodb.interfaces.PlayerOwnedObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;

@ObjectClass(Punishments.class)
public class PunishmentsService extends MongoPlayerService<Punishments> {
	private final static Map<UUID, Punishments> cache = new HashMap<>();

	public Map<UUID, Punishments> getCache() {
		return cache;
	}

	public List<Punishments> getAlts(Punishments player) {
		return getAlts(Collections.singletonList(player));
	}

	public List<Punishments> getAlts(List<Punishments> players) {
		Query<Punishments> query = database.createQuery(Punishments.class);

		List<String> ips = new ArrayList<>() {{
			for (Punishments player : players) {
				query.criteria("_id").notEqual(player.getUuid());
				addAll(player.getIps());
			}
		}};

		if (ips.isEmpty())
			return new ArrayList<>();

		query.and(query.criteria("ipHistory.ip").hasAnyOf(ips));

		return query.find().toList();
	}

	@Data
	@AllArgsConstructor
	public static class PageResult implements PlayerOwnedObject {
		@Id
		@NonNull
		private UUID uuid;
		private List<Punishment> punishments;

	}

	public List<Punishment> page(PunishmentType type, int page, int amount) {
		List<Bson> args = new ArrayList<>() {{
			add(Aggregates.unwind("$punishments"));
			add(Aggregates.replaceRoot("$punishments"));
			add(Aggregates.sort(Sorts.descending("timestamp")));
			add(Aggregates.skip((page - 1) * amount));
			add(Aggregates.limit(amount));
		}};

		if (type != null)
			args.add(2, match(eq("type", type.name())));

		return new ArrayList<>() {{
			for (Document punishment : getCollection().aggregate(args))
				add(database.getMapper().fromDBObject(database, Punishment.class, new BasicDBObject(punishment), null));
		}};
	}

}
