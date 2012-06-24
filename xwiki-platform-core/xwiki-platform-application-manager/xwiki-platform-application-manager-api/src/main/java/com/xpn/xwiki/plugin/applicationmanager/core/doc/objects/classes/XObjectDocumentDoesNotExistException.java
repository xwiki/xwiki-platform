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
package com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes;

import com.xpn.xwiki.XWikiException;

/**
 * Exception when try get {@link XObjectDocument} that does not exist.
 * 
 * @version $Id$
 * @since Application Manager 1.0RC1
 */
public class XObjectDocumentDoesNotExistException extends XWikiException
{
    /**
     * Create new instance of {@link XObjectDocumentDoesNotExistException}.
     * 
     * @param message the error message.
     */
    public XObjectDocumentDoesNotExistException(String message)
    {
        super(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, message);
    }
}
