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

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.ResourceReference;

/**
 * Interface for parsing wiki links for various wiki syntaxes.
 * <p>
 * Note: Since WikiModel doesn't parse link content we need to do it. See
 * http://code.google.com/p/wikimodel/issues/detail?id=20
 * </p>
 * 
 * @version $Id$
 * @since 1.5M2
 */
@ComponentRole
public interface LinkParser
{
    /**
     * Link Reference Type separator (eg "mailto:mail@address").
     */
    String TYPE_SEPARATOR = ":";

    /**
     * Query String separator.
     */
    String SEPARATOR_QUERYSTRING = "?";

    /**
     * Anchor separator.
     */
    String SEPARATOR_ANCHOR = "#";

    /**
     * Escape character to allow "#", "@" and "?" characters in a reference's name.
     */
    char ESCAPE_CHAR = '\\';

    /**
     * Parses a link represented as a String into a {@link org.xwiki.rendering.listener.ResourceReference} object.
     * 
     * @param rawLink the string representation of the link to parse (the supported syntax depends on the parser
     *            implementation used)
     * @return the parsed link
     */
    ResourceReference parse(String rawLink);
}
