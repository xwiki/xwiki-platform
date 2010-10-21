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
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;

/**
 * Similar to {@link org.xwiki.rendering.internal.parser.reference.DefaultResourceReferenceParser} but handles the
 * case where the resource type wasn't specified. In this case it tries to guess the type by first looking for a URL
 * and then considering it's a reference to a document.
 *
 * @version $Id$
 * @since 2.6M1
 */
@Component("link")
public class DefaultLinkReferenceParser implements ResourceReferenceParser
{
    /**
     * Default parser to parse typed resource references.
     */
    @Requirement
    private ResourceReferenceParser defaultResourceReferenceParser;

    /**
     * Used to parse untyped resource reference and guess their types.
     */
    @Requirement("link/untyped")
    private ResourceReferenceParser untypedLinkReferenceParser;

    /**
     * {@inheritDoc}
     * @see ResourceReferenceParser#parse(String)
     */
    public ResourceReference parse(String rawReference)
    {
        ResourceReference reference = this.defaultResourceReferenceParser.parse(rawReference);
        if (reference.getType().equals(ResourceType.UNKNOWN)) {
            reference = this.untypedLinkReferenceParser.parse(rawReference);
        }
        return reference;
    }
}
