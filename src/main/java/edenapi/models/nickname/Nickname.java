package edenapi.models.nickname;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import edenapi.models.PlayerOwnedObject;
import edenapi.mongodb.serializers.UUIDConverter;
import edenapi.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@Data
@Builder
@Entity(value = "nickname", noClassnameStored = true)
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class Nickname extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;

	private String nickname;
	private List<NicknameHistoryEntry> nicknameHistory = new ArrayList<>();

	public static String of(PlayerOwnedObject player) {
		return of(player.getUuid());
	}

	public static String of(UUID uuid) {
		return new NicknameService().get(uuid).getNickname();
	}

	public @NotNull String getNickname() {
		if (StringUtils.isUUID0(uuid))
			return "Console";
		if (isNullOrEmpty(nickname))
			return getName();
		return nickname;
	}

	public String getNicknameRaw() {
		return nickname;
	}

	public boolean hasNickname() {
		return !isNullOrEmpty(nickname);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NicknameHistoryEntry extends PlayerOwnedObject{
		private UUID uuid;
		private String nickname;
		private LocalDateTime requestedTimestamp;
		private String nicknameQueueId;
		private LocalDateTime responseTimestamp;
		private boolean pending = true;
		private boolean accepted;
		private boolean cancelled;
		private boolean seenResult;
		private String response;

	}

}
