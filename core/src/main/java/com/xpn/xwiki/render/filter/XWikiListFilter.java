/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @credits nested list support by Davor Cubranic
 * @credits improved nested list support by Sergiu Dumitriu
 * @author stephan
 * @author sdumitriu
 * @version $Id: XWikiListFilter.java,v 1.17 2007/01/02 13:56:14 sdumitriu Exp $
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
    private static final Map OPEN_LIST = new HashMap();

    /**
     * Maps a wiki character (bullet) to the HTML markup printed at the end of the list.
     */
    private static final Map CLOSE_LIST = new HashMap();

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
        super();
        OPEN_LIST.put(new Character('-'), "<ul class=\"minus\">\n");
        OPEN_LIST.put(new Character('*'), "<ul class=\"star\">\n");
        OPEN_LIST.put(new Character('#'), OL_OPEN);
        OPEN_LIST.put(new Character('i'), "<ol class=\"roman\">\n");
        OPEN_LIST.put(new Character('I'), "<ol class=\"ROMAN\">\n");
        OPEN_LIST.put(new Character('a'), "<ol class=\"alpha\">\n");
        OPEN_LIST.put(new Character('A'), "<ol class=\"ALPHA\">\n");
        OPEN_LIST.put(new Character('g'), "<ol class=\"greek\">\n");
        OPEN_LIST.put(new Character('h'), "<ol class=\"hiragana\">\n");
        OPEN_LIST.put(new Character('H'), "<ol class=\"HIRAGANA\">\n");
        OPEN_LIST.put(new Character('k'), "<ol class=\"katakana\">\n");
        OPEN_LIST.put(new Character('K'), "<ol class=\"KATAKANA\">\n");
        OPEN_LIST.put(new Character('j'), "<ol class=\"HEBREW\">\n");
        OPEN_LIST.put(new Character('1'), OL_OPEN);
        CLOSE_LIST.put(new Character('-'), UL_CLOSE);
        CLOSE_LIST.put(new Character('*'), UL_CLOSE);
        CLOSE_LIST.put(new Character('#'), OL_CLOSE);
        CLOSE_LIST.put(new Character('i'), OL_CLOSE);
        CLOSE_LIST.put(new Character('I'), OL_CLOSE);
        CLOSE_LIST.put(new Character('a'), OL_CLOSE);
        CLOSE_LIST.put(new Character('A'), OL_CLOSE);
        CLOSE_LIST.put(new Character('1'), OL_CLOSE);
        CLOSE_LIST.put(new Character('g'), OL_CLOSE);
        CLOSE_LIST.put(new Character('G'), OL_CLOSE);
        CLOSE_LIST.put(new Character('h'), OL_CLOSE);
        CLOSE_LIST.put(new Character('H'), OL_CLOSE);
        CLOSE_LIST.put(new Character('k'), OL_CLOSE);
        CLOSE_LIST.put(new Character('K'), OL_CLOSE);
        CLOSE_LIST.put(new Character('j'), OL_CLOSE);
    }

    /**
     * Method called whenever the wiki source matches the list syntax. It converts the wiki syntax
     * to HTML markup.
     * 
     * @param buffer The output buffer, where the HTML code is printed.
     * @param result The regex match result; input is read from this Reader.
     * @param context The FilterContext object, used to get access to the Rendering context.
     * @see org.radeox.filter.ListFilter#action(java.lang.StringBuffer,
     *      org.radeox.regex.MatchResult, org.radeox.filter.context.FilterContext)
     */
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(result.group(0)));
            addList(buffer, reader);
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
        buffer.append(OPEN_LIST.get(new Character(type)));
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
        buffer.append(CLOSE_LIST.get(new Character(type)));
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
