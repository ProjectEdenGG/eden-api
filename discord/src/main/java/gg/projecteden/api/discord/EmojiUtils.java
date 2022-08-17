package gg.projecteden.api.discord;

import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class EmojiUtils {
	public static final Emoji THUMBSUP = of("thumbsup");
	public static final Emoji WHITE_CHECK_MARK = of("white_check_mark");
	public static final Emoji X = of("x");

	public static Emoji of(String name) {
		return Emoji.fromUnicode(EmojiManager.getForAlias(name).getUnicode());
	}

}
