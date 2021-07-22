package gg.projecteden.utils;

import gg.projecteden.EdenAPI;

import java.util.Arrays;

public enum Env {
	DEV,
	TEST,
	PROD;

	public static boolean applies(Env... envs) {
		return Arrays.asList(envs).contains(EdenAPI.get().getEnv());
	}
}
