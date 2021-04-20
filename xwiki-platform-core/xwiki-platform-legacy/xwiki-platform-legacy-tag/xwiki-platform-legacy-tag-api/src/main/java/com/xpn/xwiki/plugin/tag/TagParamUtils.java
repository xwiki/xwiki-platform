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
package com.xpn.xwiki.plugin.tag;

import java.util.ArrayList;
import java.util.List;

import com.xpn.xwiki.XWikiException;

/**
 * TagParamUtils handles queries allowing to search and count tags within the wiki.
 *
 * @version $Id$
 * @since 8.2M1
 * @deprecated since 13.1RC1
 */
@Deprecated
public final class TagParamUtils
{
    /**
     * Utility class, private constructor.
     */
    private TagParamUtils()
    {
        // no instances please, static utility
    }

    /**
     * Convert a single string, describing a list of comma separated space references,
     * each single quoted, to a list of space references (without quotes).
     * Used to convert the "spaces" parameter of the tags macro.
     *
     * @param spaces string describing list of spaces
     * @return list of spaces
     * @throws com.xpn.xwiki.XWikiException if the string is not well formatted
     * @throws IllegalArgumentException if the string is null
     */
    public static List<String> spacesParameterToList(String spaces) throws XWikiException
    {
        if (spaces == null) {
            throw new IllegalArgumentException("Parameter 'spaces' should not be null");
        }
        return new HQLInListParser(spaces).getResults();
    }

    private static class HQLInListParser
    {

        private final String spaces;

        private boolean inQuotes;
        private boolean commaSeen = true;
        private int pos;
        private char c;
        private StringBuilder space;

        private final List<String> results = new ArrayList<>();

        HQLInListParser(String spaces) throws XWikiException
        {
            this.spaces = spaces;
            parse();
        }

        List<String> getResults()
        {
            return results;
        }

        private void parse() throws XWikiException
        {
            while (pos < spaces.length()) {
                c = spaces.charAt(pos++);
                if (inQuotes) {
                    handleInQuotes();
                } else {
                    handleOutsideQuotes();
                }
            }

            if (inQuotes) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                                     String.format("Missing closing quote in [%s]", spaces));
            }
            if (commaSeen && !results.isEmpty()) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                                     String.format("Unexpected comma at end of [%s]", spaces));
            }
        }

        private void handleInQuotes()
        {
            if (c == '\'') {
                if (pos < spaces.length() && spaces.charAt(pos) == '\'') {
                    pos++;
                    space.append(c);
                } else {
                    results.add(space.toString());
                    inQuotes = false;
                    commaSeen = false;
                }
            } else {
                space.append(c);
            }
        }

        private void handleOutsideQuotes() throws XWikiException
        {
            if (c == ',') {
                if (commaSeen) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                                             String.format("Unexpected comma at position %d in [%s]", pos, spaces));
                } else {
                    commaSeen = true;
                }
            } else if (c == '\'') {
                if (commaSeen) {
                    inQuotes = true;
                    space = new StringBuilder();
                } else {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                                             String.format("Unexpected quote at position %d in [%s]", pos, spaces));
                }
            } else if (!Character.isWhitespace(c)) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                                         String.format("Unexpected character `%s` at position %d in [%s]",
                                                       c, pos, spaces));
            }
        }
    }
}
