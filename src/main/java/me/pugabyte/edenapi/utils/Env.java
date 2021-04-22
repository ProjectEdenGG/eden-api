package me.pugabyte.edenapi.utils;

import me.pugabyte.edenapi.EdenAPI;

import java.util.Arrays;

public enum Env {
	DEV,
	PROD;

	public static boolean applies(Env... envs) {
		return Arrays.asList(envs).contains(EdenAPI.get().getEnv());
	}
}
