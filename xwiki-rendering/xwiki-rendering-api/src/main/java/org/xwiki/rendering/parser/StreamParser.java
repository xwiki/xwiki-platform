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

import java.io.Reader;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Streaming based parser. Instead of returning a resulting XDOM it send events during the parsing.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@ComponentRole
public interface StreamParser
{
    /**
     * @return the wiki syntax the parser is implementing
     */
    Syntax getSyntax();

    /**
     * @param source the content to parse
     * @param listener receive event for each element
     * @throws ParseException if the source cannot be read or an unexpected error happens during the parsing. Parsers
     *             should be written to not generate any error as much as possible.
     */
    void parse(Reader source, Listener listener) throws ParseException;
}
