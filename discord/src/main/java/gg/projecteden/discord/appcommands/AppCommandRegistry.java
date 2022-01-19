package gg.projecteden.discord.appcommands;

import gg.projecteden.discord.appcommands.AppCommandMeta.AppCommandMethod.AppCommandArgument;
import gg.projecteden.discord.appcommands.exceptions.AppCommandException;
import gg.projecteden.utils.CompletableFutures;
import gg.projecteden.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gg.projecteden.discord.appcommands.AppCommandHandler.parseMentions;
import static gg.projecteden.utils.Nullables.isNullOrEmpty;
import static gg.projecteden.utils.Utils.subTypesOf;

public record AppCommandRegistry(JDA jda, String packageName) {
	static final Map<String, AppCommandMeta<?>> COMMANDS = new HashMap<>();
	static final Map<Class<?>, Function<OptionMapping, Object>> OPTION_CONVERTERS = new HashMap<>();
	static final Map<Class<?>, Function<AppCommandArgumentInstance, Object>> CONVERTERS = new HashMap<>();
	static final Map<Class<?>, Supplier<List<Choice>>> CHOICES = new HashMap<>();
	static final Map<Class<?>, List<Choice>> CHOICES_CACHE = new HashMap<>();
	static final Map<Class<?>, OptionType> OPTION_TYPE_MAP = new HashMap<>();
	static final Map<Class<? extends Annotation>, BiConsumer<AppCommand, Annotation>> ANNOTATION_HANDLERS = new HashMap<>();

	static AppCommandHandler handler;

	private static final boolean debug = false;

	private static void debug(String message) {
		if (debug)
			System.out.println(message);
	}

	@SneakyThrows
	public void registerAll() {
		registerListener();
		for (var clazz : subTypesOf(AppCommand.class, packageName))
			register(clazz);
	}

	public void registerListener() {
		if (handler == null) {
			handler = new AppCommandHandler();
			jda.addEventListener(handler);
		}
	}

	public void unregisterAll() {
		unregisterGlobalCommands();
		unregisterGuildCommands();
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

		debug("/" + command.getName() + ": " + command.toData());

		for (Guild guild : jda.getGuilds()) {
			String id = "/" + command.getName() + " | " + guild.getName() + " | ";

			Consumer<String> success = action -> debug(id + "✔ " + action);
			Consumer<String> failure = action -> debug(id + "✗ " + action);

			debug(id + "Registering");

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

			Consumer<Command> setPrivilege = response -> {
				if (!meta.requiresRole())
					return;

				final List<Role> roles = guild.getRolesByName(meta.getRole(), true);
				if (roles.isEmpty()) {
					failure.accept("PRIVILEGE | " + "Required role " + meta.getRole() + " not found");
					return;
				}

				final Role requiredRole = roles.iterator().next();
				final CommandPrivilege privilege = new CommandPrivilege(Type.ROLE, true, requiredRole.getIdLong());

				guild.updateCommandPrivilegesById(response.getId(), privilege).submit()
						.thenRun(() -> success.accept("PRIVILEGE | " + requiredRole.getName() + " " + requiredRole.getId()))
						.exceptionally(ex -> {
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

	public void unregisterGuildCommands() {
		for (Guild guild : jda.getGuilds())
			unregisterGuildCommands(guild);
	}

	public void unregisterGuildCommands(Guild guild) {
		String id = " " + guild.getName() + " | ";
		Consumer<String> success = action -> debug(id + "✔ " + action);
		Consumer<String> failure = action -> debug(id + "✗ " + action);

		CompletableFutures.allOf(new ArrayList<>() {{
					guild.retrieveCommands().complete().forEach(existingCommand ->
							add(guild.deleteCommandById(existingCommand.getId()).submit()));
				}})
				.thenAcceptAsync(complete -> success.accept("DELETE EXISTING"))
				.exceptionally(ex -> {
					failure.accept("DELETE EXISTING");
					ex.printStackTrace();
					return null;
				});
	}

	private void registerGlobalCommand(AppCommandMeta<?> meta) {
		var command = meta.getCommand();

		String id = "/" + command.getName() + " | GLOBAL | ";

		Consumer<String> info = action -> debug(id + "○ " + action);
		Consumer<String> success = action -> debug(id + "✔ " + action);
		Consumer<String> failure = action -> debug(id + "✗ " + action);

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

	public void unregisterGlobalCommands() {
		Consumer<String> info = action -> debug(" GLOBAL | " + "○ " + action);
		Consumer<String> success = action -> debug(" GLOBAL | " + "✔ " + action);
		Consumer<String> failure = action -> debug(" GLOBAL | " + "✗ " + action);

		info.accept("DELETE EXISTING");
		CompletableFutures.allOf(new ArrayList<>() {{
					jda.retrieveCommands().complete().forEach(existingCommand ->
							add(jda.deleteCommandById(existingCommand.getId()).submit()));
				}})
				.thenAcceptAsync(complete -> success.accept("DELETE EXISTING"))
				.exceptionally(ex -> {
					failure.accept("DELETE EXISTING");
					ex.printStackTrace();
					return null;
				});
		;
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

	public static void registerConverter(Class<?> clazz, Function<AppCommandArgumentInstance, Object> converter) {
		registerConverter(List.of(clazz), converter);
	}

	public static void registerConverter(List<Class<?>> classes, Function<AppCommandArgumentInstance, Object> converter) {
		for (Class<?> clazz : classes)
			CONVERTERS.put(clazz, converter);
	}

	public static Function<AppCommandArgumentInstance, Object> getConverter(Class<?> clazz) {
		if (CONVERTERS.containsKey(clazz))
			return CONVERTERS.get(clazz);

		for (var converter : CONVERTERS.entrySet())
			if (converter.getKey().isAssignableFrom(clazz))
				return CONVERTERS.get(converter.getKey());

		return null;
	}

	@Data
	@AllArgsConstructor
	public static class AppCommandArgumentInstance {
		private String input;
		@NonNull
		private AppCommand command;
		@NonNull
		private AppCommandArgument meta;

	}

	static OptionType resolveOptionType(Class<?> type) {
		return OPTION_TYPE_MAP.getOrDefault(type, OptionType.STRING);
	}

	public static void supplyChoices(Class<?> clazz, Supplier<List<Choice>> supplier) {
		CHOICES.put(clazz, supplier);
	}

	static List<Choice> loadChoices(Class<?> clazz) {
		return CHOICES_CACHE.computeIfAbsent(clazz, $ -> CHOICES.getOrDefault(clazz, () -> {
			if (clazz.isEnum())
				return loadEnumChoices((Class<? extends Enum<?>>) clazz, defaultEnumChoicesFormatter());
			return Collections.emptyList();
		}).get());
	}

	private static <T extends Enum<?>> Function<T, String> defaultEnumChoicesFormatter() {
		return value -> value.name().toLowerCase().replaceAll(" ", "_");
	}

	@NotNull
	public static <T extends Enum<?>> List<Choice> loadEnumChoices(Class<? extends T> clazz, Function<T, String> formatter) {
		return Arrays.stream(clazz.getEnumConstants())
				.map(formatter)
				.map(value -> new Choice(value, value))
				.collect(Collectors.toList());
	}

	public static void registerAnnotationHandler(Class<? extends Annotation> annotation, BiConsumer<AppCommand, Annotation> consumer) {
		ANNOTATION_HANDLERS.put(annotation, consumer);
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

		registerConverter(String.class, AppCommandArgumentInstance::getInput);

		registerConverter(Enum.class, argument -> {
			final String input = argument.getInput();
			final Class<?> type = argument.getMeta().getType();

			return convertToEnum((Class<? extends Enum<?>>) type, input);
		});
	}

	@Nullable
	public static <T extends Enum<?>> T convertToEnum(Class<T> type, String input) {
		if (isNullOrEmpty(input))
			return null;

		return Arrays.stream(type.getEnumConstants())
				.filter(constant -> constant.name().equalsIgnoreCase(input))
				.findFirst()
				.orElseThrow(() -> new AppCommandException(type.getSimpleName() + " from `" + input + "` not found"));
	}

}
