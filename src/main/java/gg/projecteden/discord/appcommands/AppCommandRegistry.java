package gg.projecteden.discord.appcommands;

import gg.projecteden.utils.Utils;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege.Type;
import org.reflections.Reflections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static gg.projecteden.discord.appcommands.AppCommandHandler.parseMentions;

public record AppCommandRegistry(JDA jda, String packageName) {
	static final Map<String, AppCommandMeta<?>> COMMANDS = new HashMap<>();
	static final Map<Class<?>, Function<OptionMapping, Object>> OPTION_CONVERTERS = new HashMap<>();
	static final Map<Class<?>, Function<String, Object>> CONVERTERS = new HashMap<>();
	static final Map<Class<?>, Supplier<List<Choice>>> CHOICES = new HashMap<>();
	static final Map<Class<?>, List<Choice>> CHOICES_CACHE = new HashMap<>();
	static final Map<Class<?>, OptionType> OPTION_TYPE_MAP = new HashMap<>();

	@SneakyThrows
	public void registerAll() {
		jda.addEventListener(new AppCommandHandler());
		for (var clazz : new Reflections(packageName).getSubTypesOf(AppCommand.class))
			register(clazz);
	}

	public void register(Class<? extends AppCommand> clazz) {
		try {
			if (Utils.canEnable(clazz))
				register(new AppCommandMeta<>(clazz));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void register(AppCommandMeta<?> meta) {
		COMMANDS.put(meta.getName(), meta);

		if (meta.isGuildCommand())
			registerGuildCommand(meta);
		else
			registerGlobalCommand(meta);
	}

	private void registerGuildCommand(AppCommandMeta<?> meta) {
		var command = meta.getCommand();

		System.out.println("/" + command.getName() + ": " + command.toData());

		for (Guild guild : jda.getGuilds()) {
			String id = "/" + command.getName() + " | " + guild.getName() + " | ";

			Consumer<String> success = action -> System.out.println(id + "✔ " + action);
			Consumer<String> failure = action -> System.out.println(id + "✗ " + action);

			System.out.println(id + "Registering");

			if (!meta.getIncludedGuilds().isEmpty()) {
				if (!meta.getIncludedGuilds().contains(guild.getId())) {
					failure.accept("Not in included guilds");
					continue;
				}
			} else if (!meta.getExcludedGuilds().isEmpty()) {
				if (meta.getExcludedGuilds().contains(guild.getId())) {
					failure.accept("In excluded guilds");
					continue;
				}
			}

			/*
			guild.retrieveCommands().complete().forEach(existingCommand -> {
				guild.deleteCommandById(existingCommand.getId()).complete();
				success.accept("DELETE EXISTING /" + existingCommand.getName());
			});
			*/

			Consumer<Command> setPrivilege = response -> {
				if (!meta.requiresRole())
					return;

				final List<Role> roles = guild.getRolesByName(meta.getRole(), true);
				if (roles.isEmpty()) {
					failure.accept("PRIVILEGE | " + "Required role " + meta.getRole() + " not found");
					return;
				}

				final CommandPrivilege privilege = new CommandPrivilege(Type.ROLE, true, roles.iterator().next().getIdLong());

				guild.updateCommandPrivilegesById(response.getId(), privilege).submit().thenRun(() -> {
					success.accept("PRIVILEGE");
				}).exceptionally(ex -> {
					failure.accept("PRIVILEGE");
					ex.printStackTrace();
					return null;
				});
			};

			guild.upsertCommand(command).submit().thenAccept(response -> {
				success.accept("COMMAND");
				setPrivilege.accept(response);
			}).exceptionally(ex -> {
				failure.accept("COMMAND");
				ex.printStackTrace();
				return null;
			});
		}
	}

	private void registerGlobalCommand(AppCommandMeta<?> meta) {
		var command = meta.getCommand();

		String id = "/" + command.getName() + " | GLOBAL | ";

		Consumer<String> success = action -> System.out.println(id + "✔ " + action);
		Consumer<String> failure = action -> System.out.println(id + "✗ " + action);

		/*
		JDA.retrieveCommands().complete().forEach(existingCommand -> {
			JDA.deleteCommandById(existingCommand.getId()).complete();
			success.accept("DELETE EXISTING");
		});
		*/

		Consumer<Command> setPrivilege = response -> {
			if (!meta.requiresRole())
				return;

			// TODO
		};

		jda.upsertCommand(command).submit().thenAccept(response -> {
			success.accept("COMMAND");
			setPrivilege.accept(response);
		}).exceptionally(ex -> {
			failure.accept("COMMAND");
			ex.printStackTrace();
			return null;
		});
	}

	public static void mapOptionType(OptionType optionType, Class<?>... classes) {
		for (Class<?> clazz : classes)
			OPTION_TYPE_MAP.put(clazz, optionType);
	}

	private static void registerOptionConverter(Class<?> clazz, Function<OptionMapping, Object> converter) {
		registerOptionConverter(List.of(clazz), converter);
	}

	private static void registerOptionConverter(List<Class<?>> classes, Function<OptionMapping, Object> converter) {
		for (Class<?> clazz : classes)
			OPTION_CONVERTERS.put(clazz, converter);
	}

	public static void registerConverter(Class<?> clazz, Function<String, Object> converter) {
		registerConverter(List.of(clazz), converter);
	}

	public static void registerConverter(List<Class<?>> classes, Function<String, Object> converter) {
		for (Class<?> clazz : classes)
			CONVERTERS.put(clazz, converter);
	}

	static OptionType resolveOptionType(Class<?> type) {
		return OPTION_TYPE_MAP.getOrDefault(type, OptionType.STRING);
	}

	public static void supplyChoices(Class<?> clazz, Supplier<List<Choice>> supplier) {
		CHOICES.put(clazz, supplier);
	}

	static List<Choice> loadChoices(Class<?> clazz) {
		return CHOICES_CACHE.computeIfAbsent(clazz, $ -> CHOICES.getOrDefault(clazz, Collections::emptyList).get());
	}

	static {
		mapOptionType(OptionType.BOOLEAN, Boolean.class, Boolean.TYPE);
		mapOptionType(OptionType.INTEGER, Integer.class, Long.class, Byte.class, Short.class, Integer.TYPE, Long.TYPE, Byte.TYPE, Short.TYPE);
		mapOptionType(OptionType.NUMBER, Double.class, Float.class, Double.TYPE, Float.TYPE);
		mapOptionType(OptionType.USER, Member.class, User.class);
		mapOptionType(OptionType.CHANNEL, GuildChannel.class, MessageChannel.class);
		mapOptionType(OptionType.ROLE, Role.class);
		mapOptionType(OptionType.MENTIONABLE, IMentionable.class);

		registerOptionConverter(String.class, option -> parseMentions(option.getAsString()));
		registerOptionConverter(List.of(Boolean.class, Boolean.TYPE), OptionMapping::getAsBoolean);
		registerOptionConverter(List.of(Long.class, Long.TYPE), OptionMapping::getAsLong);
		registerOptionConverter(List.of(Integer.class, Integer.TYPE), option -> Long.valueOf(option.getAsLong()).intValue());
		registerOptionConverter(List.of(Short.class, Short.TYPE), option -> Long.valueOf(option.getAsLong()).shortValue());
		registerOptionConverter(List.of(Byte.class, Byte.TYPE), option -> Long.valueOf(option.getAsLong()).byteValue());
		registerOptionConverter(List.of(Double.class, Double.TYPE), OptionMapping::getAsDouble);
		registerOptionConverter(List.of(Float.class, Float.TYPE), option -> Double.valueOf(option.getAsDouble()).floatValue());
		registerOptionConverter(Member.class, OptionMapping::getAsMember);
		registerOptionConverter(User.class, OptionMapping::getAsUser);
		registerOptionConverter(Role.class, OptionMapping::getAsRole);
		registerOptionConverter(GuildChannel.class, OptionMapping::getAsGuildChannel);
		registerOptionConverter(MessageChannel.class, OptionMapping::getAsMessageChannel);
		registerOptionConverter(IMentionable.class, OptionMapping::getAsMentionable);
	}

}
