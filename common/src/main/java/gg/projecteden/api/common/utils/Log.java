package gg.projecteden.api.common.utils;

import lombok.Data;

// lexi TODO: wth is this class? why does this abstract class have only static methods and a Data annotation?
//  we should probably be using SLF4J's API -- pretty sure it has ways to modify the logging level at runtime
@Data
public abstract class Log {

	public static boolean debug;

	public static void debug(String message) {
		if (debug)
			System.out.println("[DEBUG] " + message);
	}

	public static void log(String message) {
		System.out.println("[LOG] " + message);
	}

	public static void warn(String message) {
		System.out.println("[WARN] " + message);
	}

	public static void severe(String message) {
		System.out.println("[SEVERE] " + message);
	}

}
