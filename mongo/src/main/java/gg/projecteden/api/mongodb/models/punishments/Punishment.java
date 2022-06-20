package gg.projecteden.api.mongodb.models.punishments;

import dev.morphia.annotations.Converters;
import gg.projecteden.api.common.utils.TimeUtils.Timespan;
import gg.projecteden.api.mongodb.interfaces.PlayerOwnedObject;
import gg.projecteden.api.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import static gg.projecteden.api.common.utils.Nullables.isNullOrEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Converters(UUIDConverter.class)
public class Punishment implements PlayerOwnedObject {
	protected UUID id;
	protected UUID uuid;
	protected UUID punisher;

	protected PunishmentType type;
	protected String reason;
	protected boolean active;

	protected LocalDateTime timestamp;
	protected int seconds;
	protected LocalDateTime expiration;
	protected LocalDateTime received;

	protected UUID remover;
	protected LocalDateTime removed;

	protected UUID replacedBy;

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

	public boolean hasBeenRemoved() {
		return removed != null;
	}

	public boolean hasBeenReceived() {
		return received != null;
	}

	public boolean hasBeenReplaced() {
		return replacedBy != null;
	}

	public String getTimeLeft() {
		if (expiration == null)
			if (seconds > 0)
				return Timespan.ofSeconds(seconds).format() + " left";
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
