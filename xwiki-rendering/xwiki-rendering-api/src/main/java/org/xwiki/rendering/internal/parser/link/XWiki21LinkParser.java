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
package org.xwiki.rendering.internal.parser.link;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ResourceType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.LinkTypeParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Parses the content of XWiki Syntax 2.1 links. The syntax for links is the same as for XWiki Syntax 2.0 (see
 * {@link XWiki20LinkParser} for syntax details) but with the following
 * differences:
 * <ul>
 * <li>Ability to plug new reference types by implementing {@link org.xwiki.rendering.parser.LinkTypeParser}s.</li>
 * <li>All references have a canonical syntax with a type prefix. For documents the type is "doc" and for URLs the
 *     type is "url". However for easiness of usage we also support implicit URLs and implicit references to
 *     documents without the prefix specified.</li>
 * <li>Interwiki links now have the following syntax: {@code interwiki:<interwiki alias>:<interwiki suffix>}.</li> 
 * </ul>
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component("xwiki/2.1")
public class XWiki21LinkParser implements LinkParser
{
    /**
     * Parser to parse link references pointing to URLs.
     */
    @Requirement("url")
    private LinkTypeParser urlLinkTypeParser;

    /**
     * Parser to parse link references pointing to documents.
     */
    @Requirement("doc")
    private LinkTypeParser documentLinkTypeParser;

    /**
     * Used to verify if we're in wiki mode or not by looking up an implementation of {@link
     * org.xwiki.rendering.wiki.WikiModel}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.parser.LinkParser#parse(String)
     */
    public ResourceReference parse(String rawLink)
    {
        // Step 1: If we're not in wiki mode then all links are URL links, except for link to images (since an image
        // link can point to an image defined as a URL.
        if (!isInWikiMode() && !rawLink.startsWith("image:")) {
            ResourceReference resourceReference = new ResourceReference();
            resourceReference.setType(ResourceType.URL);
            resourceReference.setTyped(false);
            resourceReference.setReference(rawLink);
            return resourceReference;
        }

        // Step 2: Find the type parser matching the specified prefix type (if any).
        int pos = rawLink.indexOf(TYPE_SEPARATOR);
        if (pos > -1) {
            String typePrefix = rawLink.substring(0, pos);
            String reference = rawLink.substring(pos + 1);
            try {
                LinkTypeParser parser = this.componentManager.lookup(LinkTypeParser.class, typePrefix);
                ResourceReference parsedResourceReference = parser.parse(reference);
                if (parsedResourceReference != null) {
                    return parsedResourceReference;
                }
            } catch (ComponentLookupException e) {
                // Couldn't find a link type parser for the specified type. Will try to autodiscover the type.
            }
        }

        // Step 3: There's no specific type parser found. As a convenience try to guess the link type. It can be
        // either:
        // - a URL (specified without the "url" type)
        // - a reference to a document (specified without the "doc" type)
        ResourceReference parsedResourceReference = this.urlLinkTypeParser.parse(rawLink);
        if (parsedResourceReference == null) {
            // What remains is considered to be a link to a document, use the document link type parser to parse it.
            parsedResourceReference = this.documentLinkTypeParser.parse(rawLink);
        }
        parsedResourceReference.setTyped(false);

        return parsedResourceReference;
    }

    /**
     * @return true if we're in wiki mode (ie there's no implementing class for {@link
     *         org.xwiki.rendering.wiki.WikiModel})
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
