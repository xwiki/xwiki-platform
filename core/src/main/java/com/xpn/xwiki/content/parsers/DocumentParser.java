/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
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
 */
package com.xpn.xwiki.content.parsers;

import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.content.Link;

import java.util.Iterator;

import org.apache.oro.text.regex.MalformedPatternException;

/**
 * Parse document source content as typed by the user.
 *
 * <p>Note: This is a very basic parser which currently only parses wiki links. It should probably
 * not be developed further. Instead we should migrate to a proper parser such as
 * <a href="http://wikimodel.sourceforge.net/">WikiModel</a>. In any case parsing should be done
 * with a proper parser, such as ANTLR, JavaCC, etc.</p> 
 *
 * @version $Id: $
 */
public class DocumentParser implements ContentParser
{
    /**
     * Utility class used here to apply regex patterns and get matched strings.
     */
    private Util util = new Util();

    /**
     * Parse the links contained into a raw content representing a document (as typed by the user).
     * 
     * @param contentToParse the raw document content to parse.
     * @return a list of {@link Link} objects containing the parsed links and a list of invalid
     *         link contents found, returned as a {@link ParsingResultCollection}. This allows
     *         users of this method to decide what they want to do with invalid links (like
     *         report them to the user, fix them, generate an error, etc).
     */
    public ParsingResultCollection parseLinks(String contentToParse)
    {
        ParsingResultCollection results = new ParsingResultCollection();

        LinkParser linkParser = new LinkParser();
        String pattern = "\\[(.*?)\\]";

        Iterator linkContents;
        try {
            linkContents = this.util.getAllMatches(contentToParse, pattern, 1).iterator();
        } catch (MalformedPatternException e) {
            // This should never happen as our pattern used is controlled and is well formed.
            throw new RuntimeException("Invalid pattern used [" + pattern + " for parsing links "
                + "in [" + contentToParse + "]", e);
        }

        while (linkContents.hasNext()) {
            String linkContent = (String) linkContents.next();
            Link link;
            try {
                link = linkParser.parse(linkContent);
                results.addValidElement(link);
            } catch (ContentParserException e) {
                // Failed to parse a link. Add it as an invalid element in the result
                results.addInvalidElementId(contentToParse);
            }
        }

        return results;
    }
}
