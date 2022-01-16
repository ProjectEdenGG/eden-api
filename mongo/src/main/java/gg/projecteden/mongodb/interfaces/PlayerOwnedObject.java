package gg.projecteden.mongodb.interfaces;

import gg.projecteden.interfaces.DatabaseObject;
import gg.projecteden.interfaces.HasUniqueId;
import gg.projecteden.interfaces.Nicknamed;
import gg.projecteden.mongodb.models.nerd.Nerd;
import gg.projecteden.mongodb.models.nickname.Nickname;
import gg.projecteden.mongodb.models.nickname.NicknameService;
import gg.projecteden.utils.Nullables;
import gg.projecteden.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An object owned by a player, usually for Mongo
 */
public interface PlayerOwnedObject extends DatabaseObject, Nicknamed, HasUniqueId {

	@NotNull UUID getUuid();

	/**
	 * Gets the unique ID of this object. Alias for {@link #getUuid()}, for compatibility with {@link HasUniqueId}.
	 *
	 * @return this object's unique ID
	 */
	@Override
	@NotNull
	default UUID getUniqueId() {
		return getUuid();
	}

	default @NotNull Nerd getNerd() {
		return Nerd.of(getUuid());
	}

	default @NotNull String getName() {
		return getNerd().getName();
	}

	default @NotNull String getNickname() {
		return Nickname.of(getUuid());
	}

	default Nickname getNicknameData() {
		return new NicknameService().get(getUuid());
	}

	default boolean hasNickname() {
		return !Nullables.isNullOrEmpty(getNicknameData().getNicknameRaw());
	}

	default String toPrettyString() {
		return StringUtils.toPrettyString(this);
	}

/* fuck
	default boolean equals(Object obj) {
		if (!this.getClass().equals(obj.getClass())) return false;
		return getUuid().equals(((PlayerOwnedObject) obj).getUuid());
	}
*/

}
