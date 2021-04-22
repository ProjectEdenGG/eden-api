package me.pugabyte.edenapi.persistence;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseConfig {
	private DatabaseType type;
	@Builder.Default
	private String host = "localhost";
	@Builder.Default
	private int port = 27017;
	@Builder.Default
	private String username = "root";
	@Builder.Default
	private String password = "password";
	private String prefix;
	private String modelPath;

}
