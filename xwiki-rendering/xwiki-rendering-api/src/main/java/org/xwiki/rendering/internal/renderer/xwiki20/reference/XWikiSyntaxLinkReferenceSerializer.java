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
package org.xwiki.rendering.internal.renderer.xwiki20.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;

/**
 * Generate a string representation of a Link reference, in XWiki Syntax 2.0. This implementation is pluggable by
 * using internally implementations of {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer},
 * each in charge of serializing a given {@link org.xwiki.rendering.listener.reference.ResourceType}.
 * <p>
 * Note that {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer} component implementations
 * must use a role hint equal to the XWiki Syntax id followed by "/" and then Link Type name (eg "doc" for document
 * links, "attach" for attachment links, etc).
 * </p>
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xwiki/2.0/link")
public class XWikiSyntaxLinkReferenceSerializer implements ResourceReferenceSerializer
{
    /**
     * Prefix to use for {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer} role hints.
     */
    private static final String COMPONENT_PREFIX = "xwiki/2.0";

    /**
     * Used to lookup {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer} implementations.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Default serializer to use when no serializer is found for the link type.
     */
    @Requirement("xwiki/2.0")
    private ResourceReferenceTypeSerializer defaultResourceReferenceTypeSerializer;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.reference.ResourceReferenceSerializer#serialize(
     *org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public String serialize(ResourceReference reference)
    {
        String result;

        try {
            result = this.componentManager.lookup(ResourceReferenceTypeSerializer.class,
                getLinkTypeSerializerComponentPrefix() + "/" + reference.getType().getScheme()).serialize(reference);
        } catch (ComponentLookupException e) {
            // Failed to find serializer for the passed link type. Use the default serializer.
            result = this.defaultResourceReferenceTypeSerializer.serialize(reference);
        }
        return result;
    }

    /**
     * @return the role hint prefix to use when looking up
     *         {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer} implementations.
     */
    protected String getLinkTypeSerializerComponentPrefix()
    {
        return COMPONENT_PREFIX;
    }
}
