package gg.projecteden.exceptions.postconfigured;

import gg.projecteden.exceptions.EdenException;

public class PlayerNotFoundException extends EdenException {

	public PlayerNotFoundException(String input) {
		super("Player " + input + " not found");
	}

}
