package gg.projecteden.interfaces;

import me.lexikiq.HasUniqueId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface DatabaseObject extends HasUniqueId {

	UUID getUuid();

	@Override
	default @NotNull UUID getUniqueId() {
		return getUuid();
	}

}
