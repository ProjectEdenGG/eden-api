package gg.projecteden.api.discord.appcommands.exceptions;

public class AppCommandMisconfiguredException extends AppCommandException {

	public AppCommandMisconfiguredException(String message) {
		super(message);
	}

	public AppCommandMisconfiguredException(String message, Throwable cause) {
		super(message, cause);
	}

}
