/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author sdumitriu
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.regex.MatchResult;
import org.radeox.filter.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Listfilter checks for lists in in its input. These are
 * transformed to output lists, e.g. in HTML. Recognizes
 * different lists like numbered lists, unnumbered lists,
 * greek lists, alpha lists etc.
 *
 * @credits nested list support by Davor Cubranic
 * @credits improved nested list support by Sergiu Dumitriu
 * @author stephan
 * @author sdumitriu
 * @version $Id: XWikiListFilter.java,v 1.17 2007/01/02 13:56:14 sdumitriu Exp $
 */
public class XWikiListFilter extends ListFilter implements CacheFilter {
  private static Log log = LogFactory.getLog(XWikiListFilter.class);

  private final static Map openList = new HashMap();
  private final static Map closeList = new HashMap();

  private static final String UL_CLOSE = "</ul>";
  private static final String OL_CLOSE = "</ol>";

  public XWikiListFilter() {
    super();
    openList.put(new Character('-'), "<ul class=\"minus\">\n");
    openList.put(new Character('*'), "<ul class=\"star\">\n");
    openList.put(new Character('#'), "<ol>\n");
    openList.put(new Character('i'), "<ol class=\"roman\">\n");
    openList.put(new Character('I'), "<ol class=\"ROMAN\">\n");
    openList.put(new Character('a'), "<ol class=\"alpha\">\n");
    openList.put(new Character('A'), "<ol class=\"ALPHA\">\n");
    openList.put(new Character('g'), "<ol class=\"greek\">\n");
    openList.put(new Character('h'), "<ol class=\"hiragana\">\n");
    openList.put(new Character('H'), "<ol class=\"HIRAGANA\">\n");
    openList.put(new Character('k'), "<ol class=\"katakana\">\n");
    openList.put(new Character('K'), "<ol class=\"KATAKANA\">\n");
    openList.put(new Character('j'), "<ol class=\"HEBREW\">\n");
    openList.put(new Character('1'), "<ol>");
    closeList.put(new Character('-'), UL_CLOSE);
    closeList.put(new Character('*'), UL_CLOSE);
    closeList.put(new Character('#'), OL_CLOSE);
    closeList.put(new Character('i'), OL_CLOSE);
    closeList.put(new Character('I'), OL_CLOSE);
    closeList.put(new Character('a'), OL_CLOSE);
    closeList.put(new Character('A'), OL_CLOSE);
    closeList.put(new Character('1'), OL_CLOSE);
    closeList.put(new Character('g'), OL_CLOSE);
    closeList.put(new Character('G'), OL_CLOSE);
    closeList.put(new Character('h'), OL_CLOSE);
    closeList.put(new Character('H'), OL_CLOSE);
    closeList.put(new Character('k'), OL_CLOSE);
    closeList.put(new Character('K'), OL_CLOSE);
    closeList.put(new Character('j'), OL_CLOSE);
  };
  
  /**
   * Adds a list to a buffer
   *
   * @param buffer The buffer to write to
   * @param reader Input is read from this Reader
   */
  private void addList(StringBuffer buffer, BufferedReader reader) throws IOException {
    char[] lastBullet = new char[0];
    String line = null;
    while ((line = reader.readLine()) != null) {
      // no nested list handling, trim lines:
      line = line.trim();
      if (line.length() == 0) {
        continue;
      }

      int bulletEnd = line.indexOf(' ');
      if (bulletEnd < 1) {
        continue;
      }
      if ( line.charAt(bulletEnd - 1) == '.') {
        bulletEnd--;
      }
      char[] bullet = line.substring(0, bulletEnd).toCharArray();
      // Logger.log("found bullet: ('" + new String(lastBullet) + "') '" + new String(bullet) + "'");
      // check whether we find a new list
      int sharedPrefixEnd;
      for (sharedPrefixEnd = 0 ; ; sharedPrefixEnd++) {
        if (bullet.length <= sharedPrefixEnd || lastBullet.length <= sharedPrefixEnd ||
          bullet[sharedPrefixEnd] != lastBullet[sharedPrefixEnd]) {
          break;
        }
      }

      // Close old lists
      //for (int i = sharedPrefixEnd; i < lastBullet.length; i++) {
      for (int i = lastBullet.length - 1; i >= sharedPrefixEnd; i--) {
        //Logger.log("closing " + lastBullet[i]);
        buffer.append("</li>\n");
        buffer.append(closeList.get(new Character(lastBullet[i]))).append("\n");
      }

      // Open new lists
      for (int i = sharedPrefixEnd; i < bullet.length; i++) {
        //Logger.log("opening " + bullet[i]);
        if(i > 0 && i > sharedPrefixEnd){
          buffer.append("<li class=\"innerlist\">");
        }
        buffer.append(openList.get(new Character(bullet[i])));
      }
      if(bullet.length == sharedPrefixEnd){
        buffer.append("</li>\n");
      }
      buffer.append("<li>");
      buffer.append(line.substring(line.indexOf(' ') + 1));
      lastBullet = bullet;
    }

    for (int i = lastBullet.length - 1; i >= 0; i--) {
      //Logger.log("closing " + lastBullet[i]);
      buffer.append("</li>\n");
      buffer.append(closeList.get(new Character(lastBullet[i])));
    }
  }
}
