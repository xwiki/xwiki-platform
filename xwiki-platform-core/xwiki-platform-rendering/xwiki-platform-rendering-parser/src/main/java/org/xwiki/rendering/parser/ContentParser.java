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
package org.xwiki.rendering.parser;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * A bridge between platform and rendering for centralizing parsing needs.
 *
 * @version $Id$
 * @since 6.0M2
 */
@Role
public interface ContentParser
{
    /**
     * Parse content.
     *
     * @param content the content to parse.
     * @param syntax the syntax in which the content is written.
     * @return the XDOM corresponding to the parsed content.
     * @throws ParseException when a parsing error occurs.
     * @throws MissingParserException when no parser has been found.
     */
    XDOM parse(String content, Syntax syntax) throws ParseException, MissingParserException;

    /**
     * Parse content, and add source metadata.
     *
     * @param content the content to parse.
     * @param syntax the syntax in which the content is written.
     * @param source the source entity (mostly a Document Reference) containing the parsed content.
     * @return the XDOM corresponding to the parsed content with source metadata set to source.
     * @throws ParseException when a parsing error occurs.
     * @throws MissingParserException when no parser has been found.
     */
    XDOM parse(String content, Syntax syntax, EntityReference source) throws ParseException, MissingParserException;
}
