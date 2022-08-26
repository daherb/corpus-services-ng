package de.uni_hamburg.corpora.utilities.quest;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class to deal with unicode characters/strings
 * @author bba1792, Dr. Herbert Lange
 * @version 20220826
 */
public class UnicodeTools {

    // List of unicode blocks containing combining characters
    private static final List<Character.UnicodeBlock> combiningBlocks = Arrays.asList(new Character.UnicodeBlock[]{
            Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS,
            Character.UnicodeBlock.COMBINING_DIACRITICAL_MARKS_SUPPLEMENT,
            Character.UnicodeBlock.COMBINING_HALF_MARKS,
            Character.UnicodeBlock.COMBINING_MARKS_FOR_SYMBOLS
    });

    /**
     * Combines all combining characters in a string with a space
     * @param s the string
     * @return the resulting string
     */
    public static String combineSpace(String s) {
        StringBuilder sb = new StringBuilder();
        for (int c: s.codePoints().toArray()) {
            sb.append(Character.toChars(c));
            if (combiningBlocks.contains(Character.UnicodeBlock.of(c))) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
