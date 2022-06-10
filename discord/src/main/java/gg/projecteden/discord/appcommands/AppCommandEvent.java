package gg.projecteden.discord.appcommands;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Data
@RequiredArgsConstructor
public class AppCommandEvent {
	private final SlashCommandInteractionEvent event;

}
