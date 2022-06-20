package gg.projecteden.api.common;

import gg.projecteden.api.common.utils.Env;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseConfig {
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

	public static class DatabaseConfigBuilder {

		public DatabaseConfigBuilder env(final Env env) {
			this.prefix = env == Env.PROD ? null : env.name().toLowerCase();
			return this;
		}

	}

}
