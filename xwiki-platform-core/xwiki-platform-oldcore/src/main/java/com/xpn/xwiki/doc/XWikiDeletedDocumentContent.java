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
package com.xpn.xwiki.doc;

import java.io.IOException;

import com.xpn.xwiki.XWikiException;

/**
 * The content of the stored deleted document.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public interface XWikiDeletedDocumentContent
{
    /**
     * @return the serialized version of the document
     * @throws IOException when failing to get the {@link String} content
     * @throws XWikiException when failing to get the {@link String} content
     */
    String getContentAsString() throws IOException, XWikiException;

    /**
     * @param document the document to write to or null to create a new one
     * @return restored document
     * @throws IOException when failing to read the content
     * @throws XWikiException when failing to read the content
     */
    XWikiDocument getXWikiDocument(XWikiDocument document) throws IOException, XWikiException;
}
