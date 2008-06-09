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
package com.xpn.xwiki.content.parsers;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.content.Link;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Parses wiki links in the format defined in {@link com.xpn.xwiki.content.Link}.
 *
 * @see com.xpn.xwiki.content.Link
 * @version $Id$
 */
public class LinkParser implements ContentParser
{
    /**
     * Preferred separator for separating link parts (alias, link and target).
     */
    private static final String LINK_SEPARATOR_GREATERTHAN = ">";

    /**
     * Other allowed separator for separating link parts (alias, link and target).
     */
    private static final String LINK_SEPARATOR_PIPE = "|";

    /**
     * Parse the raw content representing a Wiki link (as typed by the user) and transfer it into
     * a structured form as a {@link Link} Object.
     *
     * @param contentToParse the raw content to parse. This is the link content without the
     *        square brackets.
     * @return a {@link Link} object containing the parsed data
     * @exception ContentParserException in case of an error while parsing, like a malformed
     *           element for example
     */
    public Link parse(String contentToParse) throws ContentParserException
    {
        StringBuffer content = new StringBuffer(contentToParse.trim());

        Link link = new Link();

        // Note: It's important to parse the alias and the target in that order. See
        // {@link #parseAlias} for more detailas as to why.
        parseAlias(content, link);
        parseTarget(content, link);        

        // Parse the link reference itself. Note: the order here is also very important.
        // We parse the query string early as it can contain our special delimiter characters
        // (like "."). Note: This means that "@" characters are forbidden in the query string...
        link.setInterWikiAlias(parseElementAfterString(content, "@"));
        link.setQueryString(parseElementAfterString(content, "?"));
        link.setURI(parseURI(content));
        link.setVirtualWikiAlias(parseElementBeforeString(content, ":"));
        link.setAnchor(parseElementAfterString(content, "#"));
        link.setSpace(parseElementBeforeString(content, "."));

        // What remains in the content buffer is the page name if any. If the content is empty then
        // it means no page was specified. This is allowed and in that case when the link is
        // rendered it'll be pointing to WebHome.
        if (content.length() > 0) {
            link.setPage(content.toString());
        }

        return link;
    }

    /**
     * Find out the alias part of the full link.
     *
     * <p>Note: As it's possible to specify a target we need a way to differentiate the following
     * 2 links:
     * <ul>
     *   <li>[Web Home>_blank]  -> alias = null, link = "Web Home", target = "_blank"</li>
     *   <li>[Web Home>WebHome] -> alias = "Web Home", link = "WebHome", target = null</li>
     * </ul>
     * The rule we have chosen is to force targets to start with an underscore character ("_").
     * </p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @param link the link on which to set the alias and the delimiter symbol used
     */
    protected void parseAlias(StringBuffer content, Link link)
    {
        String alias = null;

        // An alias only exists if there's a separator ("|" or ">").
        int separatorIndex = content.indexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.indexOf(LINK_SEPARATOR_GREATERTHAN);
        } else {
            link.setUsePipeDelimiterSymbol(true);
        }

        if (separatorIndex != -1) {
            String text = content.substring(0, separatorIndex).trim();

            // Have we discovered a link or an alias?
            if (content.charAt(separatorIndex + 1) != '_') {
                alias = text;
                content.delete(0, separatorIndex + 1);
            }
        }

        link.setAlias(alias);
    }

    /**
     * Find out the target part of the full link.
     *
     * <p>Note: The target element must start with an underscore ("_"). See
     * {@link #parseAlias(StringBuffer, Link)} for more details as to why.</p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @param link the link on which to set the target and the delimiter symbol used
     * @throws ContentParserException if the target does not start with an underscore
     */
    protected void parseTarget(StringBuffer content, Link link) throws ContentParserException
    {
        String target = null;

        int separatorIndex = content.lastIndexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.lastIndexOf(LINK_SEPARATOR_GREATERTHAN);
        } else {
            link.setUsePipeDelimiterSymbol(true);
        }

        if (separatorIndex != -1) {
            target = content.substring(separatorIndex + 1).trim();
            if (!target.startsWith("_")) {
                throw new ContentParserException(
                    XWikiException.ERROR_XWIKI_CONTENT_LINK_INVALID_TARGET, "Invalid link "
                    + "format. The target element must start with an underscore, got [" + target
                    + "]");
            }
            content.delete(separatorIndex, content.length());
        }

        link.setTarget(target);
    }

    /**
     * Find out the URI part of the full link. Supported URIs are either "mailto:" or any URL
     * in the form "protocol://".
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed URI or null if no URI was specified
     * @throws ContentParserException if the URI is malformed
     */
    protected URI parseURI(StringBuffer content) throws ContentParserException
    {
        String mailtoString = "mailto:";
        String urlPattern = "://";

        URI uri = null;

        // First, look for an email URI
        int index = content.indexOf(mailtoString);
        if (index != -1) {
            String text = content.substring(index);
            try {
                uri = new URI(text);
            } catch (URISyntaxException e) {
                throw new ContentParserException(
                    XWikiException.ERROR_XWIKI_CONTENT_LINK_INVALID_URI,
                    "Invalid mailto URI [" + text + "]", e);
            }
            content.delete(index, content.length());
        } else {
            // Look for a "://" pattern
            index = content.indexOf(urlPattern);
            if (index != -1) {
                // We consider that the whole content till the "://" pattern represents a protocol.
                // Indeed, if a URL is specified then virtua wiki aliases and spaces should not be
                // allowed. This will be caught by the URL constructor class which will throw an
                // exception.
                String text = content.toString();
                try {
                    uri = new URI(new URL(text).toString());
                } catch (Exception e) {
                    throw new ContentParserException(
                        XWikiException.ERROR_XWIKI_CONTENT_LINK_INVALID_URI,
                        "Invalid URL format [" + text + "]", e);
                }
                content.setLength(0);
            }
        }

        return uri;
    }

    /**
     * Find out the element located to the left of the passed separator.
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @param separator the separator string to locate the element
     * @return the parsed element or null if the separator string wasn't found
     */
    protected String parseElementBeforeString(StringBuffer content, String separator)
    {
        String element = null;

        int index = content.indexOf(separator);
        if (index != -1) {
            element = content.substring(0, index).trim();
            content.delete(0, index + 1);
        }

        return element;
    }

    /**
     * Find out the element located to the right of the passed separator.
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @param separator the separator string to locate the element
     * @return the parsed element or null if the separator string wasn't found
     */
    protected String parseElementAfterString(StringBuffer content, String separator)
    {
        String element = null;

        int index = content.lastIndexOf(separator);
        if (index != -1) {
            element = content.substring(index + separator.length()).trim();
            content.delete(index, content.length());
        }

        return element;
    }
}
