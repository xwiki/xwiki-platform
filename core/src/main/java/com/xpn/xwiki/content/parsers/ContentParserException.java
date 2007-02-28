/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.content.parsers;

/**
 * Exception raised by Content parsers when they fail to parse some content.
 * 
 * @version $Id: $
 */
public class ContentParserException extends Exception
{
    /**
     * {@inheritDoc}
     * @see Exception#Exception()
     */
    public ContentParserException()
    {
        super();
    }

    /**
     * {@inheritDoc}
     * @see Exception#Exception(String)
     */
    public ContentParserException(String message)
    {
        super(message);
    }

    /**
     * {@inheritDoc}
     * @see Exception#Exception(String, Throwable)
     */
    public ContentParserException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    /**
     * {@inheritDoc}
     * @see Exception#Exception(Throwable)
     */
    public ContentParserException(Throwable throwable)
    {
        super(throwable);
    }
}
