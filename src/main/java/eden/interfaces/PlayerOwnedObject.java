package eden.interfaces;

import eden.models.nerd.Nerd;
import eden.models.nickname.Nickname;
import eden.models.nickname.NicknameService;
import eden.utils.StringUtils;
import me.lexikiq.HasUniqueId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A mongo database object owned by a player
 */
public interface PlayerOwnedObject extends Nicknamed, HasUniqueId {

	@NotNull UUID getUuid();

	/**
	 * Gets the unique ID of this object. Alias for {@link #getUuid()}, for compatibility with {@link HasUniqueId}.
	 * @return this object's unique ID
	 */
	@Override @NotNull
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
		return !StringUtils.isNullOrEmpty(getNicknameData().getNicknameRaw());
	}

	default String toPrettyString() {
		try {
			return StringUtils.toPrettyString(this);
		} catch (Exception ignored) {
			return this.toString();
		}
	}

/* fuck
	default boolean equals(Object obj) {
		if (!this.getClass().equals(obj.getClass())) return false;
		return getUuid().equals(((PlayerOwnedObject) obj).getUuid());
	}
*/

}
