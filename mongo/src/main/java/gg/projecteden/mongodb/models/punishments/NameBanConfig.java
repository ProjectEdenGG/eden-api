package gg.projecteden.mongodb.models.punishments;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import gg.projecteden.mongodb.interfaces.PlayerOwnedObject;
import gg.projecteden.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Entity(value = "nameban_config", noClassnameStored = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class NameBanConfig implements PlayerOwnedObject {
	@Id
	@NonNull
	protected UUID uuid;
	protected Map<UUID, List<String>> bannedNames = new HashMap<>();
	protected Set<String> bannedWords = new HashSet<>();

	public boolean playerIsBanned(UUID uuid, String name) {
		List<String> names = bannedNames.get(uuid);
		return names != null && names.contains(name);
	}

	public boolean nameIsBanned(String name) {
		for (UUID uuid : bannedNames.keySet()) {
			List<String> names = bannedNames.get(uuid);
			if (names != null && names.contains(name))
				return true;
		}

		for (String word : bannedWords)
			if (name.toLowerCase().contains(word.toLowerCase()))
				return true;

		return false;
	}

}
