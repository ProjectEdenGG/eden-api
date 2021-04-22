package eden.models.punishments;

import dev.morphia.annotations.Converters;
import eden.models.PlayerOwnedObject;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.TimeUtils.Timespan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Converters(UUIDConverter.class)
public class Punishment extends PlayerOwnedObject {
	private UUID id;
	private UUID uuid;
	private UUID punisher;

	private PunishmentType type;
	private String reason;
	private boolean active;

	private LocalDateTime timestamp;
	private int seconds;
	private LocalDateTime expiration;
	private LocalDateTime received;

	private UUID remover;
	private LocalDateTime removed;

	private UUID replacedBy;

	public boolean isActive() {
		LocalDateTime now = LocalDateTime.now();
		if (!active)
			return false;

		if (type.hasTimespan()) {
			if (timestamp != null && timestamp.isAfter(now))
				return false;
			if (expiration != null && expiration.isBefore(now))
				return false;
		}

		return true;
	}

	public boolean hasReason() {
		return !isNullOrEmpty(reason);
	}

	boolean hasBeenRemoved() {
		return removed != null;
	}

	boolean hasBeenReceived() {
		return received != null;
	}

	boolean hasBeenReplaced() {
		return replacedBy != null;
	}

	public String getTimeLeft() {
		if (expiration == null)
			if (seconds > 0)
				return Timespan.of(seconds).format() + " left";
			else
				return "forever";
		else if (hasBeenRemoved())
			return "removed";
		else if (expiration.isBefore(LocalDateTime.now()))
			return "expired";
		else
			return Timespan.of(expiration).format() + " left";
	}

	public String getTimeSince() {
		return Timespan.of(timestamp).format() + " ago";
	}

	public String getTimeSinceRemoved() {
		return Timespan.of(removed).format() + " ago";
	}

}
