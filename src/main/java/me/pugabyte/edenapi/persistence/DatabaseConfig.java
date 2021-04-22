package me.pugabyte.edenapi.persistence;

import lombok.Getter;

@Getter
public class DatabaseConfig {
	private String host;
	private int port;
	private String username;
	private String password;
	private String prefix;
	private String modelPath;

	public DatabaseConfig(String type) {}

}
