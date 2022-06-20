package gg.projecteden.api.common.utils;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
	@NotNull
	public static Pattern ignoreCasePattern(@NotNull String regex) {
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	@NotNull
	public static Matcher ignoreCaseMatcher(@NotNull String regex, @NotNull CharSequence input) {
		return ignoreCasePattern(regex).matcher(input);
	}

	public static boolean ignoreCaseMatches(@NotNull String regex, @NotNull CharSequence input) {
		return ignoreCaseMatcher(regex, input).matches();
	}
}
