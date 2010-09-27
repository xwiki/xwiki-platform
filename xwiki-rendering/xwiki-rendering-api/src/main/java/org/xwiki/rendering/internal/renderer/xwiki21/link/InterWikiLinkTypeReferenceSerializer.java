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
package org.xwiki.rendering.internal.renderer.xwiki21.link;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.link.InterWikiLinkTypeParser;
import org.xwiki.rendering.listener.InterWikiLink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.renderer.link.LinkTypeReferenceSerializer;

/**
 * Serialize a link reference pointing to an interwiki link using the format {@code (interwikialias):(interwiki path)}.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("xwiki/2.1/interwiki")
public class InterWikiLinkTypeReferenceSerializer implements LinkTypeReferenceSerializer
{
    /**
     * {@inheritDoc}
     *
     * @see LinkTypeReferenceSerializer#serialize(Link)
     */
    public String serialize(Link link)
    {
        StringBuffer result = new StringBuffer();
        result.append(link.getType().getScheme());
        result.append(LinkParser.TYPE_SEPARATOR);
        result.append(((InterWikiLink) link).getInterWikiAlias());
        result.append(InterWikiLinkTypeParser.INTERWIKI_ALIAS_SEPARATOR);
        result.append(link.getReference());
        return result.toString();
    }
}
