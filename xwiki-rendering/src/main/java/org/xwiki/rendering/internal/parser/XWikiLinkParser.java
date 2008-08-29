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
package org.xwiki.rendering.internal.parser;

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parses the content of XWiki links. The format is as follows:
 * <code>(alias[|>])(link)(@interWikiAlias)(|target)</code>, where:
 * <ul>
 *   <li><code>alias</code>: An optional string which will be displayed to the user as the link
 *       name when rendered. Example: "My Page".</li>
 *   <li><code>link</code>: The full link reference using the following syntax:
 *       <code>(reference)(#anchor)(?queryString)</code>, where:
 *       <ul>
 *         <li><code>reference</code>: The link reference. This can be either a URI in the form
 *             <code>protocol:path</code> (example: "http://xwiki.org", "mailto:john@smith.com) or
 *             a wiki page name (example: "wiki.Space.WebHome").</li>
 *         <li><code>anchor</code>: An optional anchor name pointing to an anchor defined in the
 *             referenced link. Note that in XWiki anchors are automatically created for titles.
 *             Example: "TableOfContentAnchor".</li>
 *         <li><code>queryString</code>: An optional query string for specifying parameters that
 *             will be used in the rendered URL. Example: "mydata1=5&mydata2=Hello".</li>
 *       </ul>
 *       Either the <code>link</code> or the <code>alias</code> must be specified.</li>
 *   <li><code>interWikiAlias</code>: An optional
 *       <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> alias as defined in the
 *       InterWiki Map. Example: "wikipedia"</li>
 *   <li><code>target</code>: An optional string corresponding to the HTML <code>target</code>
 *       attribute for a <code>a</code> element. This element is used when rendering the link. It
 *       defaults to opening the link in the current page. Example: "_self", "_blank"</li>
 * </ul>
 * Examples of valid wiki links:
 * <ul>
 *   <li>Hello World</li>
 *   <li>Hello World>HelloWorld</li>
 *   <li>Hello World>HelloWorld>_blank</li>
 *   <li>Hello World>http://myserver.com/HelloWorld</li>
 *   <li>Hello World>HelloWorld#Anchor</li>
 *   <li>http://myserver.com</li>
 *   <li>Hello World@Wikipedia</li>
 *   <li>mywiki:HelloWorld</li>
 *   <li>Hello World?param1=1&param2=2</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiLinkParser implements LinkParser
{
    // Implementation note: We're not using regex in general in order to provide better error
    // messages when throwing exceptions. In addition regex makes the code less readable.
    // FWIW this is the kind of regex that would need to be used:
    //   private static final Pattern LINK_PATTERN = Pattern.compile(
    //      "(?:([^\\|>]*)[\\|>])?([^\\|>]*)(?:@([^\\|>]*))?(?:[\\|>](.*))?");
    //   private static final Pattern REFERENCE_PATTERN = Pattern.compile(
    //      "(mailto:.*|http:.*)|(?:([^?#]*)[?#]?)?(?:([^#]*)[#]?)?(.*)?");

    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("[a-zA-Z0-9+.-]*://");

    private static final String MAILTO_URI_PREFIX = "mailto:";
    
    /**
     * Preferred separator for separating link parts (label, link and target).
     */
    private static final String LINK_SEPARATOR_GREATERTHAN = ">";

    /**
     * Other allowed separator for separating link parts (label, link and target).
     */
    private static final String LINK_SEPARATOR_PIPE = "|";

    public Link parse(String rawLink) throws ParseException
    {
        StringBuffer content = new StringBuffer(rawLink.trim());

        Link link = new Link();

        // Let's default the link to be a document link. If instead it's a link to a URI or to
        // an interwiki location it'll be overriden.
        link.setType(LinkType.DOCUMENT);

        // Note: It's important to parse the label and the target in that order. See
        // {@link #parseLabel} for more details as to why.
        link.setLabel(parseLabel(content));
        link.setTarget(parseTarget(content));

        // Parse the link reference itself.
        String uri = parseURI(content);
        if (uri != null) {
            link.setReference(uri);
            link.setType(LinkType.URI);
        } else {
            // Note: the order here is also very important.
            // We parse the query string early as it can contain our special delimiter characters
            // (like "."). Note: This means that "@" characters are forbidden in the query string...

            String interwikiAlias = parseElementAfterString(content, "@");
            if (interwikiAlias != null) {
                link.setInterWikiAlias(interwikiAlias);
                link.setType(LinkType.INTERWIKI);
            }

            link.setQueryString(parseElementAfterString(content, "?"));
        }

        link.setAnchor(parseElementAfterString(content, "#"));

        // What remains in the content buffer is the page name or the interwiki reference if any.
        // If the content is empty then it means no page was specified. This is allowed and in that
        // case when the link is rendered it'll be pointing to WebHome.

        // TODO: Check for invalid characters in a page

        if (link.getReference() == null) {
            link.setReference(content.toString());
        } else if (content.length() > 0) {
            throw new ParseException("Invalid link format [" + rawLink + "]");
        }

        return link;
    }

    /**
     * Find out the label part of the full link.
     *
     * <p>Note: As it's possible to specify a target we need a way to differentiate the following
     * 2 links:
     * <ul>
     *   <li>[Web Home>_blank]  -> label = null, link = "Web Home", target = "_blank"</li>
     *   <li>[Web Home>WebHome] -> label = "Web Home", link = "WebHome", target = null</li>
     * </ul>
     * The rule we have chosen is to force targets to start with an underscore character ("_").
     * </p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed label or null if no label was specified
     */
    protected String parseLabel(StringBuffer content)
    {
        String label = null;

        // A label only exists if there's a separator ("|" or ">").
        int separatorIndex = content.indexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.indexOf(LINK_SEPARATOR_GREATERTHAN);
        }

        if (separatorIndex != -1) {
            String text = content.substring(0, separatorIndex).trim();

            // Have we discovered a link or an label?
            if (content.charAt(separatorIndex + 1) != '_') {
                label = text;
                content.delete(0, separatorIndex + 1);
            }
        }

        return label;
    }

    /**
     * Find out the target part of the full link.
     *
     * <p>Note: The target element must start with an underscore ("_"). See
     * {@link #parseLabel(StringBuffer)} for more details as to why.</p>
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed target or null if no target was specified
     * @throws ParseException if the target does not start with an underscore
     */
    protected String parseTarget(StringBuffer content) throws ParseException
    {
        String target = null;

        int separatorIndex = content.lastIndexOf(LINK_SEPARATOR_PIPE);
        if (separatorIndex == -1) {
            separatorIndex = content.lastIndexOf(LINK_SEPARATOR_GREATERTHAN);
        }

        if (separatorIndex != -1) {
            target = content.substring(separatorIndex + 1).trim();
            if (!target.startsWith("_")) {
                throw new ParseException("Invalid link format. The target element must start with "
                    + "an underscore, got [" + target + "]");
            }
            content.delete(separatorIndex, content.length());
        }

        return target;
    }

    /**
     * Find out the URI part of the full link. Supported URIs are either "mailto:" or any URL
     * in the form "protocol://".
     *
     * @param content the string to parse. This parameter will be modified by the method to remove
     *                the parsed content.
     * @return the parsed URI or null if no URI was specified
     * @throws ParseException if the URI is malformed
     */
    protected String parseURI(StringBuffer content) throws ParseException
    {
        String uri = null;

        // First, look for an email URI
        if (content.indexOf(MAILTO_URI_PREFIX) == 0) {
            try {
                uri = new URI(content.toString()).toString();
            } catch (URISyntaxException e) {
                throw new ParseException("Invalid mailto URI [" + content.toString() + "]", e);
            }
            content.setLength(0);
        } else {
            // Look for a URL pattern
            Matcher matcher = URL_SCHEME_PATTERN.matcher(content.toString());
            if (matcher.lookingAt()) {
                // If a URL is specified then virtual wiki aliases and spaces should not be allowed.
                try {
                    uri = new URL(content.toString()).toString();
                } catch (Exception e) {
                    throw new ParseException("Invalid URL format [" + content.toString() + "]", e);
                }
                content.setLength(0);
            }
        }

        return uri;
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
