package gg.projecteden.discord.appcommands;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public abstract class AppCommand {
	protected final AppCommandEvent event;

	public Member member() {
		return event.getEvent().getMember();
	}

	public String name() {
		return member().getEffectiveName();
	}

	public String mention() {
		return member().getAsMention();
	}

	public Guild guild() {
		return event.getEvent().getGuild();
	}

	public TextChannel channel() {
		return event.getEvent().getTextChannel();
	}

	public Category category() {
		return channel().getParentCategory();
	}

	public Category category(String name) {
		return guild().getCategoriesByName(name, true).iterator().next();
	}

	public CompletableFuture<InteractionHook> reply(String message) {
		return event.getEvent().reply(message).submit();
	}

	public CompletableFuture<InteractionHook> reply(MessageBuilder message) {
		return event.getEvent().reply(message.build()).submit();
	}

	public CompletableFuture<InteractionHook> reply(EmbedBuilder message) {
		return event.getEvent().reply(new MessageBuilder().setEmbeds(message.build()).build()).submit();
	}

	public CompletableFuture<InteractionHook> replyEphemeral(String message) {
		return event.getEvent().deferReply(true).setContent(message).submit();
	}

	public CompletableFuture<InteractionHook> thumbsup() {
		return reply(":thumbsup:");
	}

	public CompletableFuture<InteractionHook> thumbsupEphemeral() {
		return replyEphemeral(":thumbsup:");
	}

}
