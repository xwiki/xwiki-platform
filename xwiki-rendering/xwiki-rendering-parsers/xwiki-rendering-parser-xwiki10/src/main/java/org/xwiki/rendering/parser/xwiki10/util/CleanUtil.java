/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.parser.xwiki10.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.internal.parser.xwiki10.VelocityFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Contains syntax cleaning helpers.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public final class CleanUtil
{
    /**
     * Match all the first new lines.
     */
    private static final Pattern STARTING_NL_GROUP_PATTERN = Pattern.compile("^\\n*");

    private static final Pattern STARTING_NLNOOUTPUT_GROUP_PATTERN =
        Pattern.compile("^" + VelocityFilter.NLGROUP_SPATTERN);

    /**
     * Match all the last new lines.
     */
    private static final Pattern ENDING_NL_GROUP_PATTERN = Pattern.compile("\\n*$");

    private static final Pattern ENDING_NLNOOUTPUT_GROUP_PATTERN =
        Pattern.compile(VelocityFilter.NLGROUP_SPATTERN + "$");

    /**
     * Match space, tab or new line.
     */
    private static final Pattern HTMLSPACEORNEWLINE_PATTERN = Pattern.compile("[\\s\\n]");

    /**
     * Match XWiki 1.0 escaping syntax.
     */
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\\\\\|([^\\\\])\\\\");

    /**
     * Utility class.
     */
    private CleanUtil()
    {

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
     * Remove first new lines if there is more than 0 and less or equals to the provided number.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines to match.
     * @param replaceWithSpace indicate if the removed new lines are replaced with a white space.
     * @return the converted string.
     */
    public static String removeLeadingNewLines(String content, int nb, boolean replaceWithSpace)
    {
        String cleanedContent = content;

        Matcher matcher = STARTING_NL_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? matcher.end() - matcher.start() : 0;

        if (foundNb > 0 && foundNb <= nb) {
            cleanedContent = content.substring(foundNb > nb ? nb : foundNb);
            if (replaceWithSpace) {
                cleanedContent = " " + cleanedContent;
            }
        }

        return cleanedContent;
    }

    /**
     * Remove last new lines if there is more than 0 and less or equals to the provided number.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines to match.
     * @param replaceWithSpace indicate if the removed new lines are replaced with a white space.
     * @return the converted string.
     */
    public static String removeTrailingNewLines(String content, int nb, boolean replaceWithSpace)
    {
        String cleanedContent = content;

        Matcher matcher = ENDING_NL_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? matcher.end() - matcher.start() : 0;

        if (foundNb > 0 && foundNb <= nb) {
            cleanedContent = content.substring(0, content.length() - (foundNb > nb ? nb : foundNb));
            if (replaceWithSpace) {
                cleanedContent = cleanedContent + " ";
            }
        }

        return cleanedContent;
    }

    /**
     * Check the provided string contains enough new lines at the beginning and add the need ones.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines the string need to contains at the beginning.
     * @return the converted string.
     */
    public static String setLeadingNewLines(String content, int nb)
    {
        String cleanedContent = content;

        Matcher matcher = STARTING_NLNOOUTPUT_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? StringUtils.countMatches(matcher.group(0), "\n") : 0;

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
    public static String setTrailingNewLines(String content, int nb)
    {
        String cleanedContent = content;

        Matcher matcher = ENDING_NLNOOUTPUT_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? StringUtils.countMatches(matcher.group(0), "\n") : 0;

        if (foundNb < nb) {
            cleanedContent = content + StringUtils.repeat("\n", nb - foundNb);
        }

        return cleanedContent;
    }

    /**
     * Check the provided string contains enough new lines at the end and add the need ones.
     * 
     * @param content the content to convert.
     * @param nb the number of new lines the string need to contains at the end.
     */
    public static void setTrailingNewLines(StringBuffer content, int nb)
    {
        Matcher matcher = ENDING_NLNOOUTPUT_GROUP_PATTERN.matcher(content);

        int foundNb = matcher.find() ? StringUtils.countMatches(matcher.group(0), "\n") : 0;

        if (foundNb < nb) {
            content.append(StringUtils.repeat("\n", nb - foundNb));
        }
    }

    /**
     * Remove all the first new lines.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String removeLeadingNewLines(String content)
    {
        return STARTING_NL_GROUP_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * Remove all the last new lines.
     * 
     * @param content the content to convert.
     * @return the converted string.
     */
    public static String removeTrailingNewLines(String content)
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

    public static String extractVelocity(CharSequence content, FilterContext filterContext)
    {
        return extractVelocity(content, filterContext, false, false);
    }

    public static String extractVelocity(CharSequence content, FilterContext filterContext, boolean protect,
        boolean inline)
    {
        String cleanedContent = content.toString();

        Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(cleanedContent);
        boolean velocityOpen = velocityOpenMatcher.find();
        cleanedContent = velocityOpenMatcher.replaceFirst("");
        Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedContent);
        boolean velocityClose = velocityCloseMatcher.find();
        cleanedContent = velocityCloseMatcher.replaceFirst("");

        StringBuffer buffer = new StringBuffer();

        boolean multilines = filterContext.unProtect(cleanedContent).indexOf("\n") != -1;

        if (velocityOpen) {
            VelocityFilter.appendVelocityOpen(buffer, filterContext, multilines);
        }

        if (protect) {
            buffer.append(filterContext.addProtectedContent(cleanedContent, inline));
        } else {
            buffer.append(cleanedContent);
        }

        if (velocityClose) {
            VelocityFilter.appendVelocityClose(buffer, filterContext, multilines);
        }

        return buffer.toString();
    }
}
