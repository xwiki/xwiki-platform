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
 * 
 */

/*
 * This file has been modified from the source code of
 * "SnipSnap Radeox Rendering Engine".
 *
 * Copyright (c) 2002 Stephan J. Schmidt, Matthias L. Jugel
 * All Rights Reserved.
 *
 * Please visit http://radeox.org/ for more information.
 *
 */
package com.xpn.xwiki.render.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.filter.CacheFilter;
import org.radeox.filter.ListFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.regex.MatchResult;

/**
 * Listfilter checks for lists in in its input. These are transformed to output lists, e.g. in HTML.
 * Recognizes different lists like numbered lists, unnumbered lists, greek lists, alpha lists etc.
 * 
 * @version $Id$
 */
public class XWikiListFilter extends ListFilter implements CacheFilter
{
    /**
     * Log4J logger object to log messages in this class.
     */
    private static Log log = LogFactory.getLog(XWikiListFilter.class);

    /**
     * Maps a wiki character (bullet) to the html markup printed at the start of the list.
     */
    private static final Map<Character, String> OPEN_LIST = new HashMap<Character, String>();

    /**
     * Maps a wiki character (bullet) to the HTML markup printed at the end of the list.
     */
    private static final Map<Character, String> CLOSE_LIST = new HashMap<Character, String>();

    /**
     * HTML markup for unordered list close.
     */
    private static final String UL_CLOSE = "</ul>";

    /**
     * HTML markup for ordered list open.
     */
    private static final String OL_OPEN = "<ol>\n";

    /**
     * HTML markup for ordered list close.
     */
    private static final String OL_CLOSE = "</ol>";

    /**
     * HTML markup for simple item open.
     */
    private static final String ITEM_OPEN = "<li>";

    /**
     * HTML markup for simple item close.
     */
    private static final String ITEM_CLOSE = "</li>\n";

    /**
     * Default constructor; builds predefined open and close lists.
     * 
     * @todo Maybe the syntax can be configured somehow...
     */
    public XWikiListFilter()
    {
        OPEN_LIST.put('-', "<ul class=\"minus\">\n");
        OPEN_LIST.put('*', "<ul class=\"star\">\n");
        OPEN_LIST.put('#', OL_OPEN);
        OPEN_LIST.put('i', "<ol class=\"roman\">\n");
        OPEN_LIST.put('I', "<ol class=\"ROMAN\">\n");
        OPEN_LIST.put('a', "<ol class=\"alpha\">\n");
        OPEN_LIST.put('A', "<ol class=\"ALPHA\">\n");
        OPEN_LIST.put('g', "<ol class=\"greek\">\n");
        OPEN_LIST.put('G', "<ol class=\"GREEK\">\n");
        OPEN_LIST.put('h', "<ol class=\"hiragana\">\n");
        OPEN_LIST.put('H', "<ol class=\"HIRAGANA\">\n");
        OPEN_LIST.put('k', "<ol class=\"katakana\">\n");
        OPEN_LIST.put('K', "<ol class=\"KATAKANA\">\n");
        OPEN_LIST.put('j', "<ol class=\"HEBREW\">\n");
        OPEN_LIST.put('1', OL_OPEN);
        CLOSE_LIST.put('-', UL_CLOSE);
        CLOSE_LIST.put('*', UL_CLOSE);
        CLOSE_LIST.put('#', OL_CLOSE);
        CLOSE_LIST.put('i', OL_CLOSE);
        CLOSE_LIST.put('I', OL_CLOSE);
        CLOSE_LIST.put('a', OL_CLOSE);
        CLOSE_LIST.put('A', OL_CLOSE);
        CLOSE_LIST.put('1', OL_CLOSE);
        CLOSE_LIST.put('g', OL_CLOSE);
        CLOSE_LIST.put('G', OL_CLOSE);
        CLOSE_LIST.put('h', OL_CLOSE);
        CLOSE_LIST.put('H', OL_CLOSE);
        CLOSE_LIST.put('k', OL_CLOSE);
        CLOSE_LIST.put('K', OL_CLOSE);
        CLOSE_LIST.put('j', OL_CLOSE);
    }

    /**
     * Method called whenever the wiki source matches the list syntax. It converts the wiki syntax
     * to HTML markup.
     * 
     * @param buffer The output buffer, where the HTML code is printed.
     * @param result The regex match result; input is read from this Reader.
     * @param context The FilterContext object, used to get access to the Rendering context.
     * @see ListFilter#handleMatch(StringBuffer, org.radeox.regex.MatchResult, org.radeox.filter.context.FilterContext) 
     */
    @Override
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(result.group(0)));
            addList(buffer, reader);
            // Since this filter consumes all newlines before the next piece of text, add one back to ensure the
            // following text begins on a new line, and not right after the closing list tag.
            buffer.append("\n");
        } catch (Exception e) {
            log.warn("ListFilter: unable to get list content", e);
        }
    }

    /**
     * Converts a list item to the corresponding HTML markup, possibly closing or opening the
     * neccessary lists.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param reader Input is read from this Reader.
     * @throws IOException if the input stream is broken.
     */
    private void addList(StringBuffer buffer, BufferedReader reader) throws IOException
    {
        char[] lastBullet = new char[0];
        String line = null;
        while ((line = reader.readLine()) != null) {
            // No nested list handling, trim lines:
            line = line.trim();
            int bulletEnd = line.indexOf(' ');
            if (line.length() == 0 || bulletEnd < 1) {
                continue;
            }

            if (line.charAt(bulletEnd - 1) == '.') {
                bulletEnd--;
            }
            char[] bullet = line.substring(0, bulletEnd).toCharArray();
            log.debug("found bullet: ('" + new String(lastBullet) + "') '" + new String(bullet)
                + "'");

            interpolateLists(buffer, lastBullet, bullet);
            openItem(buffer, bullet[bullet.length - 1]);
            buffer.append(line.substring(line.indexOf(' ') + 1));
            lastBullet = bullet;
        }

        for (int i = lastBullet.length - 1; i >= 0; i--) {
            closeItem(buffer, lastBullet[i]);
            closeList(buffer, lastBullet[i]);
        }
    }

    /**
     * Output the HTML code corresponding to opening a new list item.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param type The list item type.
     */
    private void openItem(StringBuffer buffer, char type)
    {
        buffer.append(ITEM_OPEN);
    }

    /**
     * Output the HTML code corresponding to closing a list item.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param type The list item type.
     */
    private void closeItem(StringBuffer buffer, char type)
    {
        buffer.append(ITEM_CLOSE);
    }

    /**
     * Output the HTML code corresponding to opening a new list.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param type The list item type.
     */
    private void openList(StringBuffer buffer, char type)
    {
        log.debug("opening " + type);
        buffer.append(OPEN_LIST.get(type));
    }

    /**
     * Output the HTML code corresponding to closing a list.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param type The list item type.
     */
    private void closeList(StringBuffer buffer, char type)
    {
        log.debug("closing " + type);
        buffer.append(CLOSE_LIST.get(type));
    }

    /**
     * Makes the transition from the previous configuration of list types to the current one,
     * closing and opening any needed lists/items.
     * 
     * @param buffer The output buffer to write HTML to.
     * @param previousBullets The previous list type configuration.
     * @param crtBullets The new list type configuration.
     */
    private void interpolateLists(StringBuffer buffer, char[] previousBullets, char[] crtBullets)
    {
        int sharedPrefixEnd;
        for (sharedPrefixEnd = 0;; sharedPrefixEnd++) {
            if ((crtBullets.length <= sharedPrefixEnd)
                || (previousBullets.length <= sharedPrefixEnd)
                || (crtBullets[sharedPrefixEnd] != previousBullets[sharedPrefixEnd])) {
                break;
            }
        }

        // Close old lists
        for (int i = previousBullets.length - 1; i >= sharedPrefixEnd; i--) {
            closeItem(buffer, previousBullets[i]);
            closeList(buffer, previousBullets[i]);
        }

        // Open new lists
        for (int i = sharedPrefixEnd; i < crtBullets.length; i++) {
            if (i > 0 && i > sharedPrefixEnd) {
                buffer.append("<li class=\"innerlist\">");
            }
            openList(buffer, crtBullets[i]);
        }
        if (crtBullets.length == sharedPrefixEnd) {
            closeItem(buffer, previousBullets[sharedPrefixEnd - 1]);
        }
    }
}
