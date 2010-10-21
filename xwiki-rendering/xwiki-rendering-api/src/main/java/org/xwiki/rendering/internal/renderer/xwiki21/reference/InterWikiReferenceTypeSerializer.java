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
import org.xwiki.rendering.internal.parser.reference.DefaultResourceReferenceParser;
import org.xwiki.rendering.internal.parser.reference.InterWikiResourceReferenceTypeParser;
import org.xwiki.rendering.listener.reference.InterWikiResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;

/**
 * Serialize a link reference pointing to an interwiki link using the format {@code (interwikialias):(interwiki path)}.
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xwiki/2.1/interwiki")
public class InterWikiReferenceTypeSerializer implements ResourceReferenceTypeSerializer
{
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer#serialize(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public String serialize(ResourceReference reference)
    {
        StringBuffer result = new StringBuffer();
        result.append(reference.getType().getScheme());
        result.append(DefaultResourceReferenceParser.TYPE_SEPARATOR);
        String interWikiAlias = reference.getParameter(InterWikiResourceReference.INTERWIKI_ALIAS);
        if (interWikiAlias != null) {
            result.append(interWikiAlias);
            result.append(InterWikiResourceReferenceTypeParser.INTERWIKI_ALIAS_SEPARATOR);
        }
        result.append(reference.getReference());
        return result.toString();
    }
}
