package gg.projecteden.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class RegexUtils {
	@NotNull
	public Pattern ignoreCasePattern(@NotNull String regex) {
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	}

	@NotNull
	public Matcher ignoreCaseMatcher(@NotNull String regex, @NotNull CharSequence input) {
		return ignoreCasePattern(regex).matcher(input);
	}

	public boolean ignoreCaseMatches(@NotNull String regex, @NotNull CharSequence input) {
		return ignoreCaseMatcher(regex, input).matches();
	}
}
