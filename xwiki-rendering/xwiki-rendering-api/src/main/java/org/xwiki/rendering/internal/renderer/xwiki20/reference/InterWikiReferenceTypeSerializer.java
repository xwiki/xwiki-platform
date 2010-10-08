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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.reference.XWiki20LinkReferenceParser;
import org.xwiki.rendering.listener.reference.InterWikiResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer;

/**
 * Serialize a link reference pointing to an interwiki link using the format
 * {@code (interwiki path)@(interwiki alias)}.
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("xwiki/2.0/interwiki")
public class InterWikiReferenceTypeSerializer implements ResourceReferenceTypeSerializer
{
    /**
     * Escapes to add when rendering a link reference part.
     */
    private static final String[] ESCAPE_REPLACEMENTS_REFERENCE = new String[]{
        "" + XWiki20LinkReferenceParser.ESCAPE_CHAR + XWiki20LinkReferenceParser.ESCAPE_CHAR };

    /**
     * Replacement chars for the escapes to add to the reference part.
     */
    private static final String[] ESCAPES_REFERENCE = new String[]{
        "" + XWiki20LinkReferenceParser.ESCAPE_CHAR };

    /**
     * Escapes to add when rendering a link query string, anchor or interwiki part.
     */
    private static final String[] ESCAPE_REPLACEMENTS_EXTRA = new String[]{
        "" + XWiki20LinkReferenceParser.ESCAPE_CHAR + XWiki20LinkReferenceParser.SEPARATOR_INTERWIKI,
        "" + XWiki20LinkReferenceParser.ESCAPE_CHAR + XWiki20LinkReferenceParser.ESCAPE_CHAR };

    /**
     * Replacement chars for the escapes to add to the query string, anchor or interwiki part.
     */
    private static final String[] ESCAPES_EXTRA = new String[]{
        XWiki20LinkReferenceParser.SEPARATOR_INTERWIKI,
        "" + XWiki20LinkReferenceParser.ESCAPE_CHAR };

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.renderer.reference.ResourceReferenceTypeSerializer#serialize(org.xwiki.rendering.listener.reference.ResourceReference)
     */
    public String serialize(ResourceReference reference)
    {
        String interWikiAlias = reference.getParameter(InterWikiResourceReference.INTERWIKI_ALIAS);
        String result = addEscapesToReferencePart(reference.getReference());
        if (interWikiAlias != null) {
            result = addEscapesToReferencePart(reference.getReference())
                + XWiki20LinkReferenceParser.SEPARATOR_INTERWIKI + addEscapesToExtraParts(interWikiAlias);
        }
        return result;
    }

    /**
     * @param text the reference to which to add escapes to
     * @return the modified text
     */
    protected String addEscapesToReferencePart(String text)
    {
        return StringUtils.replaceEach(text, ESCAPES_REFERENCE, ESCAPE_REPLACEMENTS_REFERENCE);
    }

    /**
     * @param text the query string and anchor parts to which to add escapes to
     * @return the modified text
     */
    protected String addEscapesToExtraParts(String text)
    {
        return StringUtils.replaceEach(text, ESCAPES_EXTRA, ESCAPE_REPLACEMENTS_EXTRA);
    }
}
