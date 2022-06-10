package gg.projecteden.discord.appcommands;

import gg.projecteden.discord.appcommands.AppCommandMeta.AppCommandMethod;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/*
TODO
- Exception handling
 */

public class AppCommandHandler extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		try {
			AppCommandMethod.of(event).handle(event);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected static String parseMentions(String content) {
		// TODO

		return content;
	}

}
