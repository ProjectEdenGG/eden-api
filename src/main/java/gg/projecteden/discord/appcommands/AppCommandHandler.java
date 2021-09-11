package gg.projecteden.discord.appcommands;

import gg.projecteden.discord.appcommands.AppCommandMeta.AppCommandMethod;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/*
TODO
- Exception handling
 */

public class AppCommandHandler extends ListenerAdapter {

	@Override
	public void onSlashCommand(@NotNull SlashCommandEvent event) {
		try {
			System.out.println("Handling SlashCommandEvent");
			final AppCommandMeta<?>.AppCommandMethod method = AppCommandMethod.of(event);
			System.out.println("Method: " + method.getName());
			method.handle(event);
			System.out.println("Handled");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected static String parseMentions(String content) {
		// TODO

		return content;
	}

}
