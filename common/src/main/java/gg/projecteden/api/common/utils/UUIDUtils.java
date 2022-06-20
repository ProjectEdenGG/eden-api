package gg.projecteden.api.common.utils;

import gg.projecteden.api.common.EdenAPI;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

public class UUIDUtils {
	public static final UUID UUID0 = new UUID(0, 0);
	public static final String UUID_REGEX = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}";
	public static final int VERSION_CHAR_INDEX = 14;

	public static int[] toIntArray(UUID uuid) {
		return toIntArray(uuid.toString());
	}

	public static int[] toIntArray(String uuid) {
		uuid = uuid.replace("-", "");
		int loop = 0;
		int[] intArray = new int[4];
		for (int i = 0; i < 4; i++) {
			loop = loop + 8;
			intArray[i] = (int) Long.parseLong(uuid.substring(loop - 8, loop), 16);
		}
		return intArray;
	}

	public static UUID toUUID(int[] intArray) {
		return UUID.fromString(toString(intArray));
	}

	public static String toString(int[] intArray) {
		StringBuilder uuid = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			uuid.append(Integer.toHexString(intArray[i]));
		}
		uuid.insert(8, "-");
		uuid.insert(13, "-");
		uuid.insert(18, "-");
		uuid.insert(23, "-");
		return uuid.toString();
	}

	public static boolean isUUID0(UUID uuid) {
		return UUID0.equals(uuid);
	}

	public static boolean isAppUuid(UUID uuid) {
		return EdenAPI.get().getAppUuid().equals(uuid);
	}

	public static String uuidFormat(String uuid) {
		uuid = uuidUnformat(uuid);
		String formatted = "";
		formatted += uuid.substring(0, 8) + "-";
		formatted += uuid.substring(8, 12) + "-";
		formatted += uuid.substring(12, 16) + "-";
		formatted += uuid.substring(16, 20) + "-";
		formatted += uuid.substring(20, 32);
		return formatted;
	}

	public static String uuidUnformat(String uuid) {
		return uuid.replaceAll("-", "");
	}

	@Contract("null -> false")
	public static boolean isUuid(String uuid) {
		return uuid != null && uuid.matches(UUID_REGEX);
	}

	@Contract("null -> false")
	public static boolean isV4Uuid(UUID uuid) {
		return isUuidVersion(uuid, '4');
	}

	@Contract("null -> false")
	public static boolean isV4Uuid(String uuid) {
		return isUuid(uuid) && isUuidVersion(uuid, '4');
	}

	@Contract("null -> false")
	public static boolean isV3Uuid(UUID uuid) {
		return isUuidVersion(uuid, '3');
	}

	@Contract("null -> false")
	public static boolean isV3Uuid(String uuid) {
		return isUuid(uuid) && isUuidVersion(uuid, '3');
	}

	public static boolean isUuidVersion(UUID uuid, char version) {
		return isUuidVersion(uuid.toString(), version);
	}

	public static boolean isUuidVersion(String uuid, char version) {
		return uuid.charAt(VERSION_CHAR_INDEX) == version;
	}

}
