package edenapi.utils;

import edenapi.EdenAPI;

import java.util.Arrays;

public enum Env {
	DEV,
	PROD;

	public static boolean applies(Env... envs) {
		return Arrays.asList(envs).contains(EdenAPI.get().getEnv());
	}
}
