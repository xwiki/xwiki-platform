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
package org.xwiki.rendering.internal.parser.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.ResourceReferenceTypeParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Parses the content of resource references. The format of a resource reference is the following:
 * {@code (type):(reference)} where {@code type} represents the type (see
 * {@link org.xwiki.rendering.listener.reference.ResourceType} of the resource pointed to (e.g. document, mailto,
 * attachment, image, document in another wiki, etc), and {@code reference} defines the target.
 * The syntax of {@code reference} depends on the Resource type and is documented in the javadoc of the various
 * {@link org.xwiki.rendering.parser.ResourceReferenceTypeParser} implementations.
 *
 * Note that the implementation is pluggable and it's allowed plug new resource reference types by implementing
 * {@link org.xwiki.rendering.parser.ResourceReferenceTypeParser}s and registering the implementation as a component.
 *
 * @version $Id$
 * @since 2.6M1
 */
@Component
public class DefaultResourceReferenceParser implements ResourceReferenceParser
{
    /**
     * Link Reference Type separator (eg "mailto:mail@address").
     */
    public static final String TYPE_SEPARATOR = ":";

    /**
     * Used to verify if we're in wiki mode or not by looking up an implementation of {@link
     * org.xwiki.rendering.wiki.WikiModel}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     *
     * @return the parsed resource reference or a Resource Reference with {@link ResourceType#UNKNOWN} if no reference
     *         type was specified
     *
     * @see org.xwiki.rendering.parser.ResourceReferenceParser#parse(String)
     */
    public ResourceReference parse(String rawReference)
    {
        // Step 1: If we're not in wiki mode then all links are URL links.
        if (!isInWikiMode()) {
            ResourceReference resourceReference = new ResourceReference(rawReference, ResourceType.URL);
            resourceReference.setTyped(false);
            return resourceReference;
        }

        // Step 2: Find the type parser matching the specified prefix type (if any).
        int pos = rawReference.indexOf(TYPE_SEPARATOR);
        if (pos > -1) {
            String typePrefix = rawReference.substring(0, pos);
            String reference = rawReference.substring(pos + 1);
            try {
                ResourceReferenceTypeParser
                    parser = this.componentManager.lookup(ResourceReferenceTypeParser.class, typePrefix);
                ResourceReference parsedResourceReference = parser.parse(reference);
                if (parsedResourceReference != null) {
                    return parsedResourceReference;
                }
            } catch (ComponentLookupException e) {
                // Couldn't find a link type parser for the specified type. Will try to autodiscover the type.
            }
        }

        // Step 3: There's no specific type parser found.
        return new ResourceReference(rawReference, ResourceType.UNKNOWN);
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
