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
package org.xwiki.rendering.internal.renderer.xwiki20.link;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.renderer.link.LinkReferenceSerializer;
import org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer;

/**
 * Generate a string representation of a {@link}'s reference, in XWiki Syntax 2.0. This implementation is pluggable by
 * using internally implementations of {@link org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer}, each in
 * charge of serializing a given {@link org.xwiki.rendering.listener.ResourceType}.
 * <p>
 * Note that {@link org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer} component implementations must use
 * a role hint equal to the XWiki Syntax id followed by "/" and then Link Type name (eg "doc" for document links,
 * "attach" for attachment links, etc).
 * </p>
 *
 * @version $Id$
 * @since 2.3M2
 */
@Component("xwiki/2.0")
public class XWikiSyntaxLinkReferenceSerializer implements LinkReferenceSerializer
{
    /**
     * Prefix to use for {@link org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer} role hints. 
     */
    private static final String COMPONENT_PREFIX = "xwiki/2.0";

    /**
     * Used to lookup {@link org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer} implementations.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Default serializer to use when no serializer is found for the link type.
     */
    @Requirement("xwiki/2.0")
    private LinkTypeReferenceSerializer defaultLinkTypeReferenceSerializer;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.link.LinkReferenceSerializer#serialize(
     *      org.xwiki.rendering.listener.ResourceReference)
     */
    public String serialize(ResourceReference reference)
    {
        String result;

        try {
            result = this.componentManager.lookup(LinkTypeReferenceSerializer.class,
                getLinkTypeSerializerComponentPrefix() + "/" + reference.getType().getScheme()).serialize(reference);
        } catch (ComponentLookupException e) {
            // Failed to find serializer for the passed link type. Use the default serializer.
            result = this.defaultLinkTypeReferenceSerializer.serialize(reference);
        }
        return result;
    }

    /**
     * @return the role hint prefix to use when looking up
     *         {@link org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer} implementations.
     */
    protected String getLinkTypeSerializerComponentPrefix()
    {
        return COMPONENT_PREFIX;
    }
}
