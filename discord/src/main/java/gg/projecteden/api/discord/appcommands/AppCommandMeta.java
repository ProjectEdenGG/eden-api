package gg.projecteden.api.discord.appcommands;

import gg.projecteden.api.common.exceptions.EdenException;
import gg.projecteden.api.common.utils.ReflectionUtils;
import gg.projecteden.api.discord.appcommands.AppCommandRegistry.AppCommandArgumentInstance;
import gg.projecteden.api.discord.appcommands.annotations.Choices;
import gg.projecteden.api.discord.appcommands.annotations.Command;
import gg.projecteden.api.discord.appcommands.annotations.Desc;
import gg.projecteden.api.discord.appcommands.annotations.GuildCommand;
import gg.projecteden.api.discord.appcommands.annotations.Optional;
import gg.projecteden.api.discord.appcommands.annotations.RequiredRole;
import gg.projecteden.api.discord.appcommands.exceptions.AppCommandException;
import gg.projecteden.api.discord.appcommands.exceptions.AppCommandMisconfiguredException;
import lombok.Data;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gg.projecteden.api.common.utils.Nullables.isNullOrEmpty;
import static gg.projecteden.api.common.utils.StringUtils.replaceLast;

@Data
public class AppCommandMeta<C extends AppCommand> {
	private final String name;
	private final Class<C> clazz;
	private final String role;
	private final boolean guildCommand;
	private final List<String> includedGuilds;
	private final List<String> excludedGuilds;
	private final SlashCommandData command;
	private final Map<String, AppCommandMethod> methods;

	public AppCommandMeta(Class<C> clazz) {
		this.name = replaceLast(clazz.getSimpleName(), AppCommand.class.getSimpleName(), "").toLowerCase();
		this.clazz = clazz;
		this.role = defaultRole(getAnnotation(clazz, RequiredRole.class));
		this.command = Commands.slash(name, requireDescription(clazz));
//		if (!isNullOrEmpty(role))
//			this.command.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.EMPTY_PERMISSIONS));

		GuildCommand guildAnnotation = getAnnotation(clazz, GuildCommand.class);
		if (guildAnnotation == null) {
			this.guildCommand = false;
			this.includedGuilds = Collections.emptyList();
			this.excludedGuilds = Collections.emptyList();
		} else {
			this.guildCommand = true;
			this.includedGuilds = List.of(guildAnnotation.value());
			this.excludedGuilds = List.of(guildAnnotation.exclude());
		}

		init();

		this.methods = new HashMap<>() {{
			new HashMap<String, Method>() {{
				ReflectionUtils.methodsAnnotatedWith(clazz, Command.class).forEach(method -> {
					String key = method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")) + ")";
					if (!containsKey(key))
						put(key, method);
					else if (get(key).getDeclaringClass().isAssignableFrom(method.getDeclaringClass()))
						put(key, method);
				});
			}}.values().forEach(method -> {
				method.setAccessible(true);
				final AppCommandMethod methodMeta = new AppCommandMethod(method);
				put(name + methodMeta.getPath(), methodMeta);
			});
		}};
	}

	private void init() {
		try {
			// Load class into JVM to call static initializers
			Class.forName(this.clazz.getName());
		} catch (Exception | ExceptionInInitializerError ex) {
			ex.printStackTrace();
		}
	}

	@SneakyThrows
	public C newInstance(AppCommandEvent event) {
		return clazz.getConstructor(event.getClass()).newInstance(event);
	}

	public AppCommandMethod getMethod(String commandPath) {
		return methods.get(commandPath);
	}

	@Data
	public class AppCommandMethod {
		private final Method method;
		private final String name;
		private final String description;
		private final String role;
		private final String[] literals;
		private final List<AppCommandArgument> arguments;
		private final List<OptionData> options;

		public AppCommandMethod(Method method) {
			final var annotation = method.getAnnotation(Command.class);
			this.method = method;
			this.name = method.getName();
			this.literals = annotation.literals() ? name.toLowerCase().split("_", 2) : new String[0];
			this.role = defaultRole(method.getAnnotation(RequiredRole.class));
			this.description = annotation.value();
			this.arguments = Stream.of(method.getParameters()).map(AppCommandArgument::new).toList();
			this.options = this.arguments.stream().map(AppCommandArgument::asOption).toList();
			build();
		}

		public static AppCommandMeta<?>.AppCommandMethod of(SlashCommandInteractionEvent event) {
			if(AppCommandRegistry.COMMANDS.containsKey(event.getName()))
				return AppCommandRegistry.COMMANDS.get(event.getName()).getMethod(event.getCommandPath());

			return null;
		}

		@SneakyThrows
		public void handle(SlashCommandInteractionEvent event) {
			try {
				final C command = newInstance(new AppCommandEvent(event));
				handleAnnotations(command);
				method.invoke(command, convert(command, event.getOptions()));
			} catch (Exception ex) {
				Throwable error = ex;
				if (ex instanceof InvocationTargetException)
					error = ex.getCause();

				event.deferReply(true).setContent(error.getMessage()).queue();
				if (!(error instanceof EdenException))
					error.printStackTrace();
			}
		}

		private void handleAnnotations(C command) {
			AppCommandRegistry.ANNOTATION_HANDLERS.forEach((annotation, consumer) -> {
				Annotation annotationInstance;

				annotationInstance = command.getClass().getAnnotation(annotation);
				if (annotationInstance != null)
					consumer.accept(command, annotationInstance);

				annotationInstance = method.getAnnotation(annotation);
				if (annotationInstance != null)
					consumer.accept(command, annotationInstance);
			});
		}

		@NotNull
		private Object[] convert(C command, List<OptionMapping> options) {
			checkRole(command.member(), role);

			final Object[] arguments = new Object[method.getParameters().length];

			int index = 0;
			for (AppCommandArgument argument : this.arguments)
				arguments[index++] = argument.tryConvert(command, argument.findOption(options));

			return arguments;
		}

		@Data
		public class AppCommandArgument {
			private final Parameter parameter;
			private final String name;
			private final String description;
			private final String defaultValue;
			private final String role;
			private final Class<?> type;
			private final Class<?> choices;
			private final boolean required;
			private final OptionType optionType;

			public AppCommandArgument(Parameter parameter) {
				final Choices choicesAnnotation = parameter.getAnnotation(Choices.class);
				final Optional optionalAnnotation = parameter.getAnnotation(Optional.class);
				final RequiredRole roleAnnotation = parameter.getAnnotation(RequiredRole.class);

				this.parameter = parameter;
				this.name = parameter.getName();
				this.description = defaultDescription(parameter);
				this.defaultValue = defaultValue(optionalAnnotation);
				this.role = defaultRole(roleAnnotation);
				this.type = parameter.getType();
				this.choices = choicesAnnotation == null ? type : choicesAnnotation.value();
				this.required = optionalAnnotation == null;
				this.optionType = AppCommandRegistry.resolveOptionType(this.type);
			}

			protected OptionData asOption() {
				if (!AppCommandRegistry.OPTION_CONVERTERS.containsKey(type)) {
					var converter = AppCommandRegistry.getConverter(type);
					if (converter == null)
						throw new AppCommandMisconfiguredException("No converter for " + type.getSimpleName() + " registered");
				}

				final OptionData option = new OptionData(optionType, parameter.getName().toLowerCase(), description, required);

				if (choices != null) {
					final List<Choice> choices = AppCommandRegistry.loadChoices(this.choices);
					if (choices != null && !choices.isEmpty())
						option.addChoices(choices);
				}

				return option;
			}

			protected OptionMapping findOption(List<OptionMapping> options) {
				return options.stream()
						.filter(option -> option.getName().equals(name))
						.findFirst()
						.orElse(null);
			}

			public Object tryConvert(C command, OptionMapping argument) {
				try {
					checkRole(command.member(), role);
				} catch (AppCommandException ex) {
					if (required && isNullOrEmpty(defaultValue))
						throw ex;
					else
						argument = null;
				}

				if (required && argument == null)
					throw new AppCommandException(name + " is required");

				Object object;

				if (argument != null && AppCommandRegistry.OPTION_CONVERTERS.containsKey(type))
					object = AppCommandRegistry.OPTION_CONVERTERS.get(type).apply(argument);
				else
					object = convert(command, argument);

				if (required && object == null)
					throw new AppCommandException(name + " is required");

				return object;
			}

			public Object convert(C command, OptionMapping argument) {
				String string = null;
				if (argument != null) {
					string = (String) AppCommandRegistry.OPTION_CONVERTERS.get(String.class).apply(argument);

					if (isNullOrEmpty(string))
						string = defaultValue;
				}

				final String input = string == null && !isNullOrEmpty(defaultValue) ? defaultValue : string;
				final Supplier<AppCommandArgumentInstance> argumentInstance = () -> new AppCommandArgumentInstance(input, command, this);

				if (argument != null && AppCommandRegistry.OPTION_CONVERTERS.containsKey(type))
					return AppCommandRegistry.OPTION_CONVERTERS.get(type).apply(argument);

				var converter = AppCommandRegistry.getConverter(type);
				if (converter == null)
					throw new AppCommandMisconfiguredException("No converter for " + type.getSimpleName() + " registered 2");

				return converter.apply(argumentInstance.get());
			}

		}

		private void build() {
			try {
				switch (literals.length) {
					case 0 -> command.addOptions(options);
					case 1 -> command.addSubcommands(asSubcommand(0));
					case 2 -> getGroup().addSubcommands(asSubcommand(1));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@NotNull
		private SubcommandGroupData getGroup() {
			return command.getSubcommandGroups().stream()
					.filter(group -> group.getName().equals(literals[0]))
					.findFirst()
					.orElseGet(() -> {
						final SubcommandGroupData group = new SubcommandGroupData(literals[0], description);
						command.addSubcommandGroups(group);
						return group;
					});
		}

		@NotNull
		private SubcommandData asSubcommand(int index) {
			return new SubcommandData(literals[index], description).addOptions(options);
		}

		public String getPath() {
			final String path = String.join("/", literals);
			return isNullOrEmpty(path) ? "" : "/" + path;
		}

	}

	private void checkRole(Member member, String roleName) {
		if (isNullOrEmpty(roleName))
			return;

		if (member.getRoles().stream().noneMatch(role -> roleName.equalsIgnoreCase(role.getName())))
			throw new AppCommandException("No permission");
	}

	public boolean requiresRole() {
		return !isNullOrEmpty(role);
	}

	@NotNull
	private static String requireDescription(Class<?> clazz) {
		final Command annotation = getAnnotation(clazz, Command.class);
		if (annotation == null)
			throw new AppCommandMisconfiguredException(clazz.getSimpleName() + " does not have @" + Command.class.getSimpleName());

		return annotation.value();
	}

	@NotNull
	private static String defaultDescription(Parameter parameter) {
		final Desc annotation = parameter.getAnnotation(Desc.class);
		return annotation == null ? parameter.getName() : annotation.value();
	}

	@Nullable
	private static String defaultRole(RequiredRole annotation) {
		return annotation == null ? null : annotation.value();
	}

	@Nullable
	@Contract("null -> null; !null -> _")
	private static String defaultValue(Optional annotation) {
		return annotation == null ? null : annotation.value();
	}

	@Nullable
	private static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotation) {
		A result = null;
		for (var superclass : ReflectionUtils.superclassesOf(clazz)) {
			result = superclass.getAnnotation(annotation);
			if (result != null)
				break;
		}
		return result;
	}

}
