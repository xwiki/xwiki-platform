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

import org.xwiki.rendering.syntax.Syntax;

/**
 * Specialized parser exception to report missing parser for a given syntax.
 *
 * @version $Id$
 * @since 6.0M2
 */
public class MissingParserException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Construct a new MissingParserException for the specified syntax.
     *
     * @param syntax The requested syntax. Used to compose a message for Throwable.getMessage() method.
     */
    public MissingParserException(Syntax syntax)
    {
        super(getMessage(syntax));
    }

    /**
     * Construct a new MissingParserException for the specified syntax and cause.
     *
     * @param syntax The requested syntax. Used to compose a message for Throwable.getMessage() method.
     * @param throwable the cause. This can be retrieved later by the Throwable.getCause() method. (A null value
     *                  is permitted, and indicates that the cause is nonexistent or unknown)
     */
    public MissingParserException(Syntax syntax, Throwable throwable)
    {
        super(getMessage(syntax), throwable);
    }

    private static String getMessage(Syntax syntax)
    {
        return String.format("Failed to find a parser for syntax [%s]", syntax);
    }
}
