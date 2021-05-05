package eden.models.nerd;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.exceptions.EdenException;
import eden.interfaces.PlayerOwnedObject;
import eden.mongodb.serializers.LocalDateConverter;
import eden.mongodb.serializers.LocalDateTimeConverter;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lexikiq.HasUniqueId;
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
public class Nerd implements PlayerOwnedObject {
	@Id
	@NonNull
	protected UUID uuid;
	protected String name;
	protected String preferredName;
	protected String prefix;
	protected boolean checkmark;
	protected LocalDate birthday;
	protected LocalDateTime firstJoin;
	protected LocalDateTime lastJoin;
	protected LocalDateTime lastQuit;
	protected LocalDate promotionDate;
	protected String about;
	protected boolean meetMeVideo;
	protected Set<String> pronouns = new HashSet<>();
	protected static final LocalDateTime EARLIEST_JOIN = LocalDateTime.of(2015, 1, 1, 0, 0);

	protected Set<String> aliases = new HashSet<>();
	protected Set<String> pastNames = new HashSet<>();

	public static Nerd of(String name) {
		return new NerdService().get(name);
	}

	public static Nerd of(HasUniqueId player) {
		return new NerdService().get(player);
	}

	public static Nerd of(UUID uuid) {
		return new NerdService().get(uuid);
	}

	public @NotNull String getName() {
		if (StringUtils.isUUID0(uuid))
			return "Console";
		if (name == null) {
			try {
				throw new EdenException("Stacktrace");
			} catch (EdenException ex) {
				ex.printStackTrace();
			}
			return "api-" + getUuid().toString();
		}
		return name;
	}

}
