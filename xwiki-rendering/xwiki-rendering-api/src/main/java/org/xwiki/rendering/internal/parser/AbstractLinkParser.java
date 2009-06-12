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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Generic parsing of link content. This class is a helper class for classes implementing link partsers. 
 * The supported format is as follows: <code>(link)(@interWikiAlias)?</code>, where:
 * <ul>
 * <li><code>link</code>: The full link reference using the following syntax:
 * <code>(reference)(#anchor)?(?queryString)?</code>, where:
 * <ul>
 * <li><code>reference</code>: The link reference. This can be either a URI in the form <code>protocol:path</code>
 * (example: "http://xwiki.org", "mailto:john@smith.com) or a wiki page name (example: "wiki:Space.WebHome").</li>
 * <li><code>anchor</code>: An optional anchor name pointing to an anchor defined in the referenced link. Note that in
 * XWiki anchors are automatically created for titles. Example: "TableOfContentAnchor".</li>
 * <li><code>queryString</code>: An optional query string for specifying parameters that will be used in the rendered
 * URL. Example: "mydata1=5&mydata2=Hello".</li>
 * </ul>
 * The <code>link</code> element is mandatory.</li>
 * <li><code>interWikiAlias</code>: An optional <a href="http://en.wikipedia.org/wiki/InterWiki">Inter Wiki</a> alias as
 * defined in the InterWiki Map. Example: "wikipedia"</li>
 * </ul>
 * Examples of valid wiki links:
 * <ul>
 * <li>Hello World</li>
 * <li>http://myserver.com/HelloWorld</li>
 * <li>HelloWorld#Anchor</li>
 * <li>Hello World@Wikipedia</li>
 * <li>mywiki:HelloWorld</li>
 * <li>Hello World?param1=1&param2=2</li>
 * </ul>
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractLinkParser  implements LinkParser
{
    // Implementation note: We're not using regex in general in order to provide better error
    // messages when throwing exceptions. In addition regex makes the code less readable.
    // FWIW this is the kind of regex that would need to be used:
    // private static final Pattern LINK_PATTERN = Pattern.compile(
    // "(?:([^\\|>]*)[\\|>])?([^\\|>]*)(?:@([^\\|>]*))?(?:[\\|>](.*))?");
    // private static final Pattern REFERENCE_PATTERN = Pattern.compile(
    // "(mailto:.*|http:.*)|(?:([^?#]*)[?#]?)?(?:([^#]*)[#]?)?(.*)?");

    /**
     * URL matching pattern.
     */
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("[a-zA-Z0-9+.-]*://");

    /**
     * Used to verify if we're in wiki mode or not by looking up an implementation of {@link WikiModel}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * @return the list of URI prefixes the link parser recognizes
     */
    protected abstract List<String> getAllowedURIPrefixes();
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.LinkParser#parse(java.lang.String)
     */
    public Link parse(String rawLink)
    {
        StringBuffer content = new StringBuffer(rawLink.trim());

        Link link = new Link();

        // If we're not in wiki mode then all links are URI links.
        if (!isInWikiMode()) {
            link.setType(LinkType.URI);
            link.setReference(rawLink);
            return link;
        }
        
        // Let's default the link to be a document link. If instead it's a link to a URI or to
        // an interwiki location it'll be overriden.
        link.setType(LinkType.DOCUMENT);

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

            link.setAnchor(parseElementAfterString(content, "#"));

            // What remains in the content buffer is the page name or the interwiki reference if any.
            // If the content is empty then it means no page was specified. This is allowed and in that
            // case it'll be the renderer deciding what to print.

            // TODO: Check for invalid characters in a page

            link.setReference(content.toString());
        }

        return link;
    }

    /**
     * Find out the URI part of the full link. Supported URIs are those from {@link #getAllowedURIPrefixes()} 
     * or any URL in the form "protocol://".
     * 
     * @param content the string to parse. This parameter will be modified by the method to remove the parsed content.
     * @return the parsed URI or null if no URI was specified
     */
    protected String parseURI(StringBuffer content)
    {
        String uri = null;

        // First, look for one of the known URI schemes
        int uriSchemeDelimiter = content.indexOf(":");
        if ((uriSchemeDelimiter > -1) && getAllowedURIPrefixes().contains(content.substring(0, uriSchemeDelimiter))) {
            // Note: we could have been tempted to use new URI() to validate the URI. The only downside
            // is that we would need to encode the content with the defined XWiki encoding as otherwise
            // it could contain invalid characters such as spaces. It's easier and better not to perform
            // any checks here and instead let the browser handle the URI.
            uri = content.toString();
            content.setLength(0);
        } else {
            // Look for a URL pattern
            Matcher matcher = URL_SCHEME_PATTERN.matcher(content.toString());
            if (matcher.lookingAt()) {
                // We don't parse the URL since it can contain unknown protocol for the JVM but protocols known by the
                // browser
                // (such as skype:// for example).
                uri = content.toString();
                content.setLength(0);
            }
        }

        return uri;
    }

    /**
     * Find out the element located to the right of the passed separator.
     * 
     * @param content the string to parse. This parameter will be modified by the method to remove the parsed content.
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
    
    /**
     * @return true if we're in wiki mode (ie there's no implementing class for {@link WikiModel})
     */
    private boolean isInWikiMode()
    {
        boolean result = true;
        try {
            this.componentManager.lookup(WikiModel.class);
        } catch (ComponentLookupException e) {
            result = false;
        }
        return result;
    }
}
