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
package org.xwiki.rendering.internal.parser.wikimodel;

import java.io.StringReader;
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.parser.WikiModelXHTMLParser;
import org.xwiki.rendering.internal.parser.WikiModelXWiki20Parser;
import org.xwiki.rendering.listener.InlineFilterListener;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.util.ParserUtils;

/**
 * Methods for helping in parsing.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class WikiModelParserUtils extends ParserUtils
{
    public List<Block> parseInline(Parser parser, String content) throws ParseException
    {
        List<Block> result;

        // TODO: Use an inline parser instead. See http://jira.xwiki.org/jira/browse/XWIKI-2748

        // We want the XWiki parser to consider we're inside a paragraph already since links can only
        // happen in paragraph and for example if there's a macro specified as the label it should
        // generate an inline macro and not a standalone one. To force this we're explicitely adding
        // a paragraph and a word as the first content of the string to be parsed and we're removing it
        // afterwards.
        if (WikiModelXWiki20Parser.class.isAssignableFrom(parser.getClass())) {
            result = parser.parse(new StringReader("xwikimarker " + content)).getChildren();
        } else if (WikiModelXHTMLParser.class.isAssignableFrom(parser.getClass())) {
            // If the content is already inside a paragraph then simply add the "xwikimarker" prefix since
            // otherwise we would have a paragrahp inside a paragraph which would break the reason for
            // using a prefix.
            String contentToParse = "<p>xwikimarker ";
            if (content.startsWith("<p>")) {
                contentToParse = contentToParse + content.substring(3);
            } else {
                contentToParse = contentToParse + content + "</p>";
            }
            result = parser.parse(new StringReader(contentToParse)).getChildren();
        } else {
            result = parser.parse(new StringReader(content)).getChildren();
        }

        // Remove top level paragraph since we're already inside a paragraph.
        // TODO: Remove when http://code.google.com/p/wikimodel/issues/detail?id=87 is fixed
        removeTopLevelParagraph(result);

        // Remove our marker which is always the first 2 blocks (onWord("xwikimarker") + onSpace)
        if (WikiModelXWiki20Parser.class.isAssignableFrom(parser.getClass())
            || (WikiModelXHTMLParser.class.isAssignableFrom(parser.getClass()))) {
            result.remove(0);
            result.remove(0);
        }

        return result;
    }

    public void parseInline(StreamParser parser, String content, Listener listener) throws ParseException
    {
        WrappingListener inlineFilterListener = new InlineFilterListener()
        {
            private boolean foundWord = false;

            private boolean foundSpace = false;

            @Override
            public void onWord(String word)
            {
                if (foundWord) {
                    super.onWord(word);
                } else {
                    foundWord = true;
                }
            }

            @Override
            public void onSpace()
            {
                if (foundSpace) {
                    super.onSpace();
                } else {
                    foundSpace = true;
                }
            }
        };
        inlineFilterListener.setWrappedListener(listener);

        parser.parse(new StringReader("wikimarker " + content), inlineFilterListener);
    }
}
