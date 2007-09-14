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
package com.xpn.xwiki.content.parsers;

import com.xpn.xwiki.XWikiException;

/**
 * Exception raised by Content parsers when they fail to parse some content.
 * 
 * @version $Id: $
 */
public class ContentParserException extends XWikiException
{
    /**
     * {@inheritDoc}
     * @see XWikiException#XWikiException(int, int, String, Throwable, Object[])
     */
    public ContentParserException(int code, String message, Throwable e, Object[] args)
    {
        super(XWikiException.MODULE_XWIKI_CONTENT, code, message, e, args);
    }

    /**
     * {@inheritDoc}
     * @see XWikiException#XWikiException(int, int, String, Throwable)
     */
    public ContentParserException(int code, String message, Throwable e)
    {
        super(XWikiException.MODULE_XWIKI_CONTENT, code, message, e);
    }

    /**
     * {@inheritDoc}
     * @see XWikiException#XWikiException(int, int, String)
     */
    public ContentParserException(int code, String message)
    {
        super(XWikiException.MODULE_XWIKI_CONTENT, code, message);
    }

    /**
     * {@inheritDoc}
     * @see XWikiException#XWikiException() 
     */
    public ContentParserException()
    {
        super();
    }
}
