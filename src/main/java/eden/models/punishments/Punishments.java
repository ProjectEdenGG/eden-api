package eden.models.punishments;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.models.PlayerOwnedObject;
import eden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Data
@Builder
@Entity(value = "punishments", noClassnameStored = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class Punishments extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<Punishment> punishments = new ArrayList<>();
	private List<IPHistoryEntry> ipHistory = new ArrayList<>();

	public static Punishments of(String name) {
		return new PunishmentsService().get(name);
	}

	public static Punishments of(PlayerOwnedObject player) {
		return new PunishmentsService().get(player);
	}

	public static Punishments of(UUID uuid) {
		return new PunishmentsService().get(uuid);
	}

	public Punishment getById(UUID id) {
		return punishments.stream().filter(punishment -> punishment.getId().equals(id)).findFirst().orElse(null);
	}

	public boolean hasHistory() {
		return !punishments.isEmpty();
	}

	// TODO Other player IP Ban check - service query IP history
	public Optional<Punishment> getAnyActiveBan() {
		return getMostRecentActive(PunishmentType.BAN, PunishmentType.ALT_BAN);
	}

	public Optional<Punishment> getActiveBan() {
		return getMostRecentActive(PunishmentType.BAN);
	}

	public Optional<Punishment> getActiveAltBan() {
		return getMostRecentActive(PunishmentType.ALT_BAN);
	}

	public Optional<Punishment> getActiveMute() {
		return getMostRecentActive(PunishmentType.MUTE);
	}

	public Optional<Punishment> getActiveFreeze() {
		return getMostRecentActive(PunishmentType.FREEZE);
	}

	public Optional<Punishment> getActiveWatchlist() {
		return getMostRecentActive(PunishmentType.WATCHLIST);
	}

	public Optional<Punishment> getLastWarn() {
		return getMostRecentActive(PunishmentType.WARN);
	}

	public List<Punishment> getActive(PunishmentType... types) {
		return punishments.stream()
				.filter(punishment -> punishment.isActive() && Arrays.asList(types).contains(punishment.getType()))
				.collect(toList());
	}

	public Optional<Punishment> getMostRecentActive(PunishmentType... types) {
		return getMostRecent(getActive(types));
	}

	public Optional<Punishment> getMostRecent() {
		return getMostRecent(punishments);
	}

	public Optional<Punishment> getCooldown(UUID punisher) {
		Optional<Punishment> mostRecent = getMostRecent();
		if (!mostRecent.isPresent())
			return Optional.empty();

		boolean recent = mostRecent.get().getTimestamp().isAfter(LocalDateTime.now().minusSeconds(30));
		boolean samePunisher = mostRecent.get().getPunisher().equals(punisher);

		if (!(recent && !samePunisher))
			return Optional.empty();

		return mostRecent;
	}

	public Optional<Punishment> getMostRecent(List<Punishment> punishments) {
		return punishments.stream().max(Comparator.comparing(Punishment::getTimestamp));
	}

	public List<Punishment> getNewWarnings() {
		return getActive(PunishmentType.WARN).stream()
				.filter(punishment -> !punishment.hasBeenReceived())
				.collect(toList());
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IPHistoryEntry {
		private String ip;
		private LocalDateTime timestamp;

	}

	public Optional<IPHistoryEntry> getIpHistoryEntry(String ip) {
		return ipHistory.stream().filter(entry -> entry.getIp().equals(ip)).findFirst();
	}

	public boolean hasIp(String ip) {
		return getIpHistoryEntry(ip).isPresent();
	}

	public void logIp(String ip) {
		logIp(ip, LocalDateTime.now());
	}

	public void logIp(String ip, LocalDateTime timestamp) {
		if (!hasIp(ip))
			ipHistory.add(new IPHistoryEntry(ip, timestamp));
	}

	public List<String> getIps() {
		return ipHistory.stream().map(IPHistoryEntry::getIp).collect(toList());
	}

	@NotNull
	public Set<UUID> getAlts() {
		final PunishmentsService service = new PunishmentsService();
		Set<UUID> alts = new HashSet<UUID>() {{
			add(uuid);
		}};
		Set<UUID> newMatches = new HashSet<>(alts);

		int size = 1;
		while (true) {
			Set<UUID> toSearch = new HashSet<>(newMatches);
			newMatches.clear();

			List<Punishments> players = toSearch.stream().map(Punishments::of).collect(toList());
			newMatches.addAll(service.getAlts(players).stream()
					.map(Punishments::getUuid)
					.collect(toList()));

			newMatches.removeAll(alts);
			alts.addAll(newMatches);

			if (alts.size() == size)
				break;
			size = alts.size();
		}

		return alts;
	}

}
