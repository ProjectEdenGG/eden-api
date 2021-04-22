package eden.models.nerd;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.models.PlayerOwnedObject;
import eden.mongodb.serializers.LocalDateConverter;
import eden.mongodb.serializers.LocalDateTimeConverter;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity(value = "nerd", noClassnameStored = true)
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

	public @NotNull String getName() {
		if (StringUtils.isUUID0(uuid))
			return "Console";
		return name;
	}

}
