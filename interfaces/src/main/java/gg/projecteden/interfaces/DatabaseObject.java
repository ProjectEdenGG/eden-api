package gg.projecteden.interfaces;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface DatabaseObject extends HasUniqueId {

	UUID getUuid();

	@Override
	default @NotNull UUID getUniqueId() {
		return getUuid();
	}

}
