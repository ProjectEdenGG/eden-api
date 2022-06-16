package gg.projecteden.api.common.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EdenException extends RuntimeException {

	public EdenException(String message) {
		super(message);
	}

	public EdenException(String message, Throwable cause) {
		super(message, cause);
	}

}
