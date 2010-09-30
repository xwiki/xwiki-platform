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
import org.xwiki.rendering.listener.DocumentLink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;

/**
 * Parses a link reference to a document.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("doc")
public class DocumentLinkTypeParser extends AbstractURILinkTypeParser
{
    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.internal.parser.link.AbstractURILinkTypeParser#getType() 
     */
    public LinkType getType()
    {
        return LinkType.DOCUMENT;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractURILinkTypeParser#parse(String) 
     */
    @Override
    public Link parse(String reference)
    {
        DocumentLink resultLink = new DocumentLink();
        resultLink.setReference(reference);
        return resultLink;
    }
}
