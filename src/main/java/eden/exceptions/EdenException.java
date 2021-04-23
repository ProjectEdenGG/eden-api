package eden.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EdenException extends RuntimeException {

	public EdenException(String message) {
		super(message);
	}

}
