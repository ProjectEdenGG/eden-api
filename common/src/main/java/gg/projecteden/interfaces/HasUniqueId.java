package gg.projecteden.interfaces;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an object that has a {@link UUID}.
 */
@SuppressWarnings("deprecation")
public interface HasUniqueId extends OptionalUniqueId {
	/**
	 * Returns a unique and persistent id for this object
	 *
	 * @return unique id
	 */
	@NotNull UUID getUniqueId(); // named getUniqueId to maintain compatibility with Bukkit
}
