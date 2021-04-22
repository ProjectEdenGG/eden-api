package eden.exceptions.postconfigured;

import eden.exceptions.EdenException;

public class PlayerNotFoundException extends EdenException {

	public PlayerNotFoundException(String input) {
		super("Player " + input + " not found");
	}

}
