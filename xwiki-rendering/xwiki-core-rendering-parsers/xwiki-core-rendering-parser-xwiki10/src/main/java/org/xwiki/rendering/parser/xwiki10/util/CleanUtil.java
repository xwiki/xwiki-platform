package org.xwiki.rendering.parser.xwiki10.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class CleanUtil
{
    private static final Pattern STARTING_NL_GROUP_PATTERN = Pattern.compile("^\\n*", Pattern.DOTALL);

    private static final Pattern ENDING_NL_GROUP_PATTERN = Pattern.compile("\\n*$", Pattern.DOTALL);

    private static final Pattern ENDS_WITH_NL_PATTERN = Pattern.compile("[^\\n]\\n$|^\\n$", Pattern.DOTALL);

    private static final Pattern HTMLSPACEORNEWLINE_PATTERN = Pattern.compile("[\\s\\n]");

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\\\\\|([^\\\\])\\\\", Pattern.DOTALL);

    public static String removeLastStandaloneNewLine(String content)
    {
        if (ENDS_WITH_NL_PATTERN.matcher(content).matches()) {
            return content.substring(0, content.length() - 1) + ' ';
        } else {
            return content;
        }
    }

    public static String cleanSpacesAndNewLines(String content)
    {
        return HTMLSPACEORNEWLINE_PATTERN.matcher(content).replaceAll(" ");
    }

    public static String setFirstNL(String content, int nb)
    {
        String cleanedContent = content;

        Matcher matcher = STARTING_NL_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? matcher.end() - matcher.start() : 0;

        if (foundNb < nb) {
            cleanedContent = StringUtils.repeat("\n", nb - foundNb) + content;
        }

        return cleanedContent;
    }

    public static String setLastNL(String content, int nb)
    {
        String cleanedContent = content;

        Matcher matcher = ENDING_NL_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? matcher.end() - matcher.start() : 0;

        if (foundNb < nb) {
            cleanedContent = content + StringUtils.repeat("\n", nb - foundNb);
        }

        return cleanedContent;
    }

    public static String removeFirstNL(String content)
    {
        return STARTING_NL_GROUP_PATTERN.matcher(content).replaceAll("");
    }

    public static String removeLastNL(String content)
    {
        return ENDING_NL_GROUP_PATTERN.matcher(content).replaceAll("");
    }

    public static String convertEscape(String content)
    {
        return ESCAPE_PATTERN.matcher(content).replaceAll("$1~");
    }
}
