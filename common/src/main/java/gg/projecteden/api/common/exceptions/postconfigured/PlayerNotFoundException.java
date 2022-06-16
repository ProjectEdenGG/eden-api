package gg.projecteden.api.common.exceptions.postconfigured;

import gg.projecteden.api.common.exceptions.EdenException;

public class PlayerNotFoundException extends EdenException {

	public PlayerNotFoundException(String input) {
		super("Player " + input + " not found");
	}

}
