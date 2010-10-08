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
package org.xwiki.rendering.internal.renderer.xwiki21.reference;

import org.xwiki.component.annotation.Component;

/**
 * Generate a string representation of a {@link}'s reference, in XWiki Syntax 2.1.
 *
 * @version $Id$
 * @since 2.3M2
 */
@Component("xwiki/2.1/link")
public class XWikiSyntaxLinkReferenceSerializer
    extends org.xwiki.rendering.internal.renderer.xwiki20.reference.XWikiSyntaxLinkReferenceSerializer
{
    /**
     * Prefix to use for {@link org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer} role hints.
     */
    private static final String COMPONENT_PREFIX = "xwiki/2.1";

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.renderer.xwiki20.reference.XWikiSyntaxLinkReferenceSerializer#getLinkTypeSerializerComponentPrefix()
     */
    @Override
    protected String getLinkTypeSerializerComponentPrefix()
    {
        return COMPONENT_PREFIX;
    }
}
