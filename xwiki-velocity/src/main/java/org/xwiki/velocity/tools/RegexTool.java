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
package org.xwiki.velocity.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Velocity Tool offering various Regex-based APIs to make it easy to manipulate regular expressions from Velocity.
 * 
 * @version $Id$
 * @since 2.0RC2
 */
public class RegexTool
{
    /**
     * Result of a Regex search.
     */
    public class RegexResult
    {
        /**
         * @see #getStart()
         */
        public int start;

        /**
         * @see #getEnd()
         */
        public int end;

        /**
         * @see #getGroup()
         */
        public String group;

        /**
         * @param start see {@link #getStart()}
         * @param end see {@link #getEnd()}
         * @param group see {@link #getGroup()}
         */
        public RegexResult(int start, int end, String group)
        {
            this.start = start;
            this.end = end;
            this.group = group;
        }

        /**
         * @return the captured group
         */
        public String getGroup()
        {
            return this.group;
        }

        /**
         * @return the capture group's start position
         */
        public int getStart()
        {
            return this.start;
        }

        /**
         * @return the capture group's end position
         */
        public int getEnd()
        {
            return this.end;
        }
    }

    /**
     * @param content the content to parse
     * @param regex the regex to look for in the passed content
     * @return empty list if the passed regex doesn't match the content or several {@link RegexResult} objects
     *         containing the matched position and matched content for all capturing groups, the first group
     *         representing the whole . The first object is represents the entire pattern
     */
    public List<RegexResult> find(String content, String regex)
    {
        List<RegexResult> result = new ArrayList<RegexResult>();
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(content);
        if (matcher.find()) {
            for (int i = 0; i < matcher.groupCount() + 1; i++) {
                result.add(new RegexResult(matcher.start(i), matcher.end(i), matcher.group(i)));
            }
        }
        return result;
    }

    /**
     * Compiles a regular expression into a java {@code Pattern} object.
     * 
     * @param regex the textual representation of the regular expression
     * @return the {@code Pattern} object corresponding to the regular expression, or {@code null} if the expression is
     *         invalid
     * @since 2.3M1
     */
    public Pattern compile(String regex)
    {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException ex) {
            return null;
        }
    }

    /**
     * Returns a literal pattern <code>String</code> for the specified <code>String</code>.
     * <p>
     * This method produces a <code>String</code> that can be used to create a <code>Pattern</code> that would match the
     * string <code>s</code> as if it were a literal pattern.
     * </p>
     * Metacharacters or escape sequences in the input sequence will be given no special meaning.
     * 
     * @param s The string to be literalized
     * @return A literal string replacement
     * @since 2.4M2
     */
    public String quote(String s)
    {
        return Pattern.quote(s);
    }
}
