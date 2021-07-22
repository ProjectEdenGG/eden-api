package gg.projecteden.models.punishments;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
public enum PunishmentType {
	BAN("banned", true, true, false, true),
	ALT_BAN("alt-banned", true, true, false, true),
	KICK("kicked", false, false, true, true),
	MUTE("muted", true, true, false, false),
	WARN("warned", false, false, false, false),
	FREEZE("froze", false, true, true, true),
	WATCHLIST("watchlisted", false, true, true, true);

	private final String pastTense;
	@Accessors(fluent = true)
	private final boolean hasTimespan;
	private final boolean onlyOneActive;
	private final boolean automaticallyReceived;
	private final boolean receivedIfAfk;

}
