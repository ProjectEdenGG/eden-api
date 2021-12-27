package gg.projecteden.interfaces;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an object that may have a {@link UUID}.
 * @deprecated Use Adventure's <a href="https://jd.adventure.kyori.net/api/4.9.3/net/kyori/adventure/pointer/Pointered.html">Pointered class</a>
 * with <a href="https://jd.adventure.kyori.net/api/4.9.3/net/kyori/adventure/identity/Identity.html#UUID">Identity.UUID</a>
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated(since = "2.0.0")
public interface OptionalUniqueId {
	/**
	 * Returns a unique and persistent id for this object which may be null
	 *
	 * @return unique id or null
	 */
	@Nullable UUID getUniqueId(); // named getUniqueId to maintain compatibility with Bukkit
}
