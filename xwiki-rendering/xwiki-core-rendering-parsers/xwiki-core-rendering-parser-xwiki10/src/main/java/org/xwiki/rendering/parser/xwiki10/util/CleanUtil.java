package org.xwiki.rendering.parser.xwiki10.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Contains syntax cleaning helpers.
 * 
 * @version $Id$
 */
public class CleanUtil
{
    /**
     * Match all the first new lines.
     */
    private static final Pattern STARTING_NL_GROUP_PATTERN = Pattern.compile("^\\n*");

    /**
     * Match all the last new lines.
     */
    private static final Pattern ENDING_NL_GROUP_PATTERN = Pattern.compile("\\n*$");

    /**
     * Match the last unique last new line. Does not match if there is more that one new line.
     */
    private static final Pattern ENDS_WITH_NL_PATTERN = Pattern.compile("[^\\n]\\n$|^\\n$");

    /**
     * Match space, tab or new line.
     */
    private static final Pattern HTMLSPACEORNEWLINE_PATTERN = Pattern.compile("[\\s\\n]");

    /**
     * Match XWiki 1.0 escaping syntax.
     */
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\\\\\|([^\\\\])\\\\");

    /**
     * Remove last new line if there is only one new line.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String removeLastStandaloneNewLine(String content)
    {
        if (ENDS_WITH_NL_PATTERN.matcher(content).matches()) {
            return content.substring(0, content.length() - 1) + ' ';
        } else {
            return content;
        }
    }

    /**
     * Replace all spaces/new line groupes by one space.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String cleanSpacesAndNewLines(String content)
    {
        return HTMLSPACEORNEWLINE_PATTERN.matcher(content).replaceAll(" ");
    }

    /**
     * Check the provided string contains enough new lines at the beginning and add the need ones.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines the string need to contains at the beginning.
     * @return the converted string.
     */
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

    /**
     * Check the provided string contains enough new lines at the end and add the need ones.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines the string need to contains at the end.
     * @return the converted string.
     */
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

    /**
     * Remove all the first new lines.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String removeFirstNL(String content)
    {
        return STARTING_NL_GROUP_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * Remove all the last new lines.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String removeLastNL(String content)
    {
        return ENDING_NL_GROUP_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String convertEscape(String content)
    {
        return ESCAPE_PATTERN.matcher(content).replaceAll("$1~");
    }
}
