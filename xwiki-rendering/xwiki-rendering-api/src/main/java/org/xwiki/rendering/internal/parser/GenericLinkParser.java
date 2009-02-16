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

import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.parser.LinkParser;

/**
 * Since we need to have wiki syntax-specific link parsers, this generic parser allows at least to the reference
 * displayed when using syntaxes other than XWiki (which has its specific link parser, see {@link XWikiLinkParser}),
 * while waiting for specialized link parsers to be written.
 *
 * @version $Id$
 * @since 1.6RC1
 */
public class GenericLinkParser implements LinkParser
{
    /**
     * {@inheritDoc}
     * @see LinkParser#parse(String)
     */
    public Link parse(String rawLink)
    {
        Link link = new Link();
        link.setReference(rawLink);
        return link;
    }
}
