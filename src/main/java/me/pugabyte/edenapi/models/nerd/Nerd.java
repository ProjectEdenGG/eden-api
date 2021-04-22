package me.pugabyte.edenapi.models.nerd;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.edenapi.models.PlayerOwnedObject;
import me.pugabyte.edenapi.mongodb.serializers.LocalDateConverter;
import me.pugabyte.edenapi.mongodb.serializers.LocalDateTimeConverter;
import me.pugabyte.edenapi.mongodb.serializers.UUIDConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity("nerd")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, LocalDateConverter.class, LocalDateTimeConverter.class})
public class Nerd extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private String name;
	private String preferredName;
	private String prefix;
	private boolean checkmark;
	private LocalDate birthday;
	private LocalDateTime firstJoin;
	private LocalDateTime lastJoin;
	private LocalDateTime lastQuit;
	private LocalDate promotionDate;
	private String about;
	private boolean meetMeVideo;
	private Set<String> pronouns = new HashSet<>();
	private static final LocalDateTime EARLIEST_JOIN = LocalDateTime.of(2015, 1, 1, 0, 0);

	private Set<String> aliases = new HashSet<>();
	private Set<String> pastNames = new HashSet<>();

	public static Nerd of(PlayerOwnedObject player) {
		return of(player.getUuid());
	}

	public static Nerd of(UUID uuid) {
		return new NerdService().get(uuid);
	}

}
