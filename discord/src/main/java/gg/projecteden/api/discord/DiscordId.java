package gg.projecteden.api.discord;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static gg.projecteden.api.common.utils.StringUtils.camelCase;

public final class DiscordId {

	@Getter
	@AllArgsConstructor
	public enum TextChannel implements ISnowflake {
		INFO(819817990249644032L),
		ANNOUNCEMENTS(133970047382061057L),
		SOCIAL_MEDIA(1000953029069131786L),
		CHANGELOG(819818214313689098L),
		BOOSTS(846814263754620938L),

		GENERAL(132680070480396288L),
		BIRTHDAYS(968229444777046056L),
		BOTS(223897739082203137L),

		SURVIVAL(709504495373123594L),
		MINIGAMES(133970350957395968L),
		ONEBLOCK(831645582405533726L),
		CREATIVE(831645551073165332L),

		BRIDGE(331277920729432065L),
		STAFF_BRIDGE(331842528854802443L),
		STAFF_OPERATORS(151881902813478914L),
		STAFF_ADMINS(133950052249894913L),
		STAFF_LOG(256866302176526336L),
		ADMIN_LOG(390751748261937152L),

		STAFF_SOCIAL_MEDIA(525363810811248666L),
		STAFF_PROMOTIONS(133949148251553792L),
		STAFF_WATCHLIST(134162415536308224L),
		STAFF_NICKNAME_QUEUE(824454559756713994L),

		ARCHIVED_OPS_BRIDGE(331846903266279424L),
		TEST(241774576822910976L),
		;

		private final long idLong;

		public net.dv8tion.jda.api.entities.StandardGuildMessageChannel get(JDA jda) {
			if (this == ANNOUNCEMENTS)
				return Guild.PROJECT_EDEN.get(jda).getNewsChannelById(idLong);
			else
				return Guild.PROJECT_EDEN.get(jda).getTextChannelById(idLong);
		}

		public static TextChannel of(net.dv8tion.jda.api.entities.TextChannel textChannel) {
			return of(textChannel.getId());
		}

		public static TextChannel of(String id) {
			for (TextChannel textChannel : values())
				if (textChannel.getId().equals(id))
					return textChannel;

			return null;
		}
	}

	public enum VoiceChannelCategory {
		THEATRE,
		GENERAL,
		MINIGAMES,
		STAFF,
		AFK,
		;

		public Set<VoiceChannel> getAll() {
			return Arrays.stream(VoiceChannel.values())
					.filter(voiceChannel -> voiceChannel.getCategory() == this)
					.collect(Collectors.toSet());
		}

		public Set<String> getIds() {
			return getAll().stream().map(VoiceChannel::getId).collect(Collectors.toSet());
		}
	}

	@Getter
	@AllArgsConstructor
	public enum VoiceChannel implements ISnowflake {
		MINIGAMES(VoiceChannelCategory.MINIGAMES, 133782271822921728L),
		RED(VoiceChannelCategory.MINIGAMES, 133785819432353792L),
		BLUE(VoiceChannelCategory.MINIGAMES, 133785864890351616L),
		GREEN(VoiceChannelCategory.MINIGAMES, 133785943772495872L),
		YELLOW(VoiceChannelCategory.MINIGAMES, 133785902680899585L),
		WHITE(VoiceChannelCategory.MINIGAMES, 360496040501051392L),
		;

		VoiceChannel(VoiceChannelCategory category, long id) {
			this(category, id, null);
		}

		private final VoiceChannelCategory category;
		private final long idLong;
		private final String permission;

		public net.dv8tion.jda.api.entities.VoiceChannel get(JDA jda) {
			return Guild.PROJECT_EDEN.get(jda).getVoiceChannelById(idLong);
		}

		public static VoiceChannel of(net.dv8tion.jda.api.entities.VoiceChannel voiceChannel) {
			return of(voiceChannel.getId());
		}

		public static VoiceChannel of(String id) {
			for (VoiceChannel voiceChannel : values())
				if (voiceChannel.getId().equals(id))
					return voiceChannel;

			return null;
		}
	}

	@Getter
	@AllArgsConstructor
	public enum User implements ISnowflake {
		GRIFFIN(115552359458799616L),
		POOGATEST(719574999673077912L),
		RELAY(352231755551473664L),
		KODA(223794142583455744L),
		UBER(85614143951892480L),
		;

		private final long idLong;

		public net.dv8tion.jda.api.entities.User get(JDA jda) {
			Member member = getMember(jda);
			return member == null ? null : member.getUser();
		}

		public net.dv8tion.jda.api.entities.Member getMember(JDA jda) {
			return Guild.PROJECT_EDEN.get(jda).retrieveMemberById(idLong).complete();
		}
	}

	@Getter
	@AllArgsConstructor
	public enum Guild implements ISnowflake {
		PROJECT_EDEN(132680070480396288L);

		private final long idLong;

		public net.dv8tion.jda.api.entities.Guild get(JDA jda) {
			return Objects.requireNonNull(jda.getGuildById(idLong), "Guild not retrieve " + camelCase(this) + " guild");
		}
	}

	@Getter
	@AllArgsConstructor
	public enum Role implements ISnowflake {
		OWNER(133668441717604352L),
		ADMINS(133751307096817664L),
		OPERATORS(133668040553267208L),
		SENIOR_STAFF(230043171407527938L),
		MODERATORS(133686548959985664L),
		STAFF(230043287782817792L),
		ARCHITECTS(363273789527818242L),
		BUILDERS(194866089761570816L),
		VETERAN(244865244512518146L),

		SUPPORTER(269916102199476236L),
		VERIFIED(411658569444884495L),
		NERD(387029729401896980L),
		KODA(331634959351545857L),

		MINIGAME_NEWS(404031494453985282L),
		MOVIE_GOERS(583293370085015553L),
		CODING_LESSONS(847258306150268951L),
		BEAR_FAIR_PARTICIPANT(469666444888506378L),

		PRONOUN_SHE_HER(832842527157649429L),
		PRONOUN_THEY_THEM(849137731242164264L),
		PRONOUN_HE_HIM(849138401059012639L),
		PRONOUN_XE_XEM(866306382281048094L),
		PRONOUN_ANY(832853171142524948L),

		STAFF_BIRTHDAY(968228226109415474L),
		BIRTHDAY(968225150610862110L),
		;

		private final long idLong;

		public net.dv8tion.jda.api.entities.Role get(JDA jda) {
			return Guild.PROJECT_EDEN.get(jda).getRoleById(idLong);
		}

		public static Role of(net.dv8tion.jda.api.entities.Role role) {
			return of(role.getId());
		}

		public static Role of(String id) {
			for (Role role : values())
				if (role.getId().equals(id))
					return role;

			return null;
		}
	}

}
