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
package org.xwiki.rendering.executor;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Parse the passed content and execute it.
 *
 * @param <T> the context to use to execute the content (e.g. Macro Transformation Context)
 * @version $Id$
 * @since 8.4RC1
 */
@Role
public interface ContentExecutor<T>
{
    /**
     * Parse and execute content.
     *
     * @param content the content to parse and execute
     * @param syntax the syntax in which the content is written.
     * @param context the context to use to execute the content (e.g. Macro Transformation Context)
     * @return the XDOM corresponding to the executed content.
     * @throws ParseException when a parsing error occurs.
     * @throws MissingParserException when no parser has been found.
     * @throws ContentExecutorException when the execution failed in some way
     */
    XDOM execute(String content, Syntax syntax, T context)
        throws ParseException, MissingParserException, ContentExecutorException;

    /**
     * Parse and execute content.
     *
     * @param content the content to parse and execute
     * @param syntax the syntax in which the content is written.
     * @param source the source entity (mostly a Document Reference) containing the parsed content.
     * @param context the context to use to execute the content (e.g. Macro Transformation Context)
     * @return the XDOM corresponding to the executed content with source metadata set to source.
     * @throws ParseException when a parsing error occurs.
     * @throws MissingParserException when no parser has been found.
     * @throws ContentExecutorException when the execution failed in some way
     */
    XDOM execute(String content, Syntax syntax, EntityReference source, T context)
        throws ParseException, MissingParserException, ContentExecutorException;
}
