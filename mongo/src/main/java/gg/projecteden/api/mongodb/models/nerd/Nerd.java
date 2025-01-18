package gg.projecteden.api.mongodb.models.nerd;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import gg.projecteden.api.common.utils.UUIDUtils;
import gg.projecteden.api.interfaces.HasUniqueId;
import gg.projecteden.api.mongodb.interfaces.PlayerOwnedObject;
import gg.projecteden.api.mongodb.serializers.LocalDateConverter;
import gg.projecteden.api.mongodb.serializers.LocalDateTimeConverter;
import gg.projecteden.api.mongodb.serializers.UUIDConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static gg.projecteden.api.common.utils.Nullables.isNullOrEmpty;

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
	protected LocalDateTime lastUnvanish;
	protected LocalDateTime lastVanish;
	protected LocalDate promotionDate;
	protected String about;
	protected boolean meetMeVideo;

	protected Set<Pronoun> pronouns = new HashSet<>();
	protected List<String> preferredNames = new ArrayList<>();
	protected Set<String> aliases = new HashSet<>();
	protected Set<String> pastNames = new HashSet<>();

	protected static final LocalDateTime EARLIEST_JOIN = LocalDateTime.of(2015, 1, 1, 0, 0);

	public static Nerd of(String name) {
		return new NerdService().get(name);
	}

	public static Nerd of(HasUniqueId player) {
		return new NerdService().get(player);
	}

	public static Nerd of(UUID uuid) {
		return new NerdService().get(uuid);
	}

	@PostLoad
	void fix() {
		if (!isNullOrEmpty(preferredName)) {
			preferredNames.add(preferredName);
			preferredName = null;
		}
	}

	public @NotNull String getName() {
		if (UUIDUtils.isUUID0(uuid))
			return "Console";
		if (name == null)
			return "api-" + getUuid();
		return name;
	}

	public enum Pronoun {
		SHE_HER,
		THEY_THEM,
		HE_HIM,
		XE_XEM,
		ANY,
		ASK,
		;

		@Override
		public String toString() {
			return format(name());
		}

		public static String format(String input) {
			if (input == null) return null;
			return input.replaceAll("_", "/").toLowerCase();
		}

		public static Pronoun of(String input) {
			if (input == null) return null;
			for (Pronoun pronoun : values())
				if (pronoun.toString().contains(format(input)))
					return pronoun;
			return null;
		}
	}

	public List<String> getFilteredPreferredNames() {
		return preferredNames.stream().filter(name -> !name.equalsIgnoreCase(getNickname())).toList();
	}

}
