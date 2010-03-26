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
package org.xwiki.rendering.internal.renderer;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;

/**
 * Generate a string representation of a {@link}'s reference using the format:
 * {@code (reference)[#anchor][?queryString][@interwikialias]}.

 * @version $Id$
 * @since 2.1M1
 */
public class BasicLinkRenderer
{
    /**
     * Escapes to add when rendering a link reference.
     */
    private static final String[] ESCAPE_REPLACEMENTS = new String[] {
        LinkParser.ESCAPE_CHAR + LinkParser.SEPARATOR_QUERYSTRING,
        LinkParser.ESCAPE_CHAR + LinkParser.SEPARATOR_INTERWIKI,
        LinkParser.ESCAPE_CHAR + LinkParser.SEPARATOR_ANCHOR};

    /**
     * Replacement chars for the escapes to add.
     */
    private static final String[] ESCAPES = new String[] {
        LinkParser.SEPARATOR_QUERYSTRING,
        LinkParser.SEPARATOR_INTERWIKI,
        LinkParser.SEPARATOR_ANCHOR};

    /**
     * @param link the link for which to generate a string representation
     * @return the string representation using the format:
     *         {@code (reference)[#anchor][?queryString][@interwikialias]}.
     */
    public String renderLinkReference(Link link)
    {
        StringBuilder buffer = new StringBuilder();

        if (link.getReference() != null) {
            // Make sure we escape special chars: #, @ and ? as they have special meaning in links, but only for
            // links to documents
            String normalizedReference = link.getReference();
            if (link.getType() == LinkType.DOCUMENT) {
                normalizedReference = StringUtils.replaceEach(link.getReference(), ESCAPES, ESCAPE_REPLACEMENTS);
            }
            buffer.append(normalizedReference);
        }
        if (link.getAnchor() != null) {
            buffer.append('#');
            buffer.append(link.getAnchor());
        }
        if (link.getQueryString() != null) {
            buffer.append('?');
            buffer.append(link.getQueryString());
        }
        if (link.getInterWikiAlias() != null) {
            buffer.append('@');
            buffer.append(link.getInterWikiAlias());
        }

        return buffer.toString();
    }
}
