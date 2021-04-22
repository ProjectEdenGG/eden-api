package me.pugabyte.edenapi.models;

import me.pugabyte.edenapi.models.nerd.Nerd;
import me.pugabyte.edenapi.models.nickname.Nickname;
import me.pugabyte.edenapi.models.nickname.NicknameService;
import me.pugabyte.edenapi.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A mongo database object owned by a player
 */
public abstract class PlayerOwnedObject {

	public abstract UUID getUuid();

	public Nerd getNerd() {
		return Nerd.of(getUuid());
	}

	public @NotNull String getName() {
		return getNerd().getName();
	}

	public @NotNull String getNickname() {
		return Nickname.of(getUuid());
	}

	protected Nickname getNicknameData() {
		return new NicknameService().get(getUuid());
	}

	public boolean hasNickname() {
		return !StringUtils.isNullOrEmpty(getNicknameData().getNicknameRaw());
	}

	public String toPrettyString() {
		try {
			return StringUtils.toPrettyString(this);
		} catch (Exception ignored) {
			return this.toString();
		}
	}

	public boolean equals(Object obj) {
		if (!this.getClass().equals(obj.getClass())) return false;
		return getUuid().equals(((PlayerOwnedObject) obj).getUuid());
	}
}
