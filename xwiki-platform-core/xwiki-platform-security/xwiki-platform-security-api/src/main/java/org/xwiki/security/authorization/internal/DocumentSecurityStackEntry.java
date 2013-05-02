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
package org.xwiki.security.authorization.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.bridge.DocumentModelBridge;

/**
 * This is an entry that stores an actual context document.
 *
 * @version $Id$
 * @since 4.3M2
 */
public class DocumentSecurityStackEntry implements SecurityStackEntry
{

    /** The document encapsulated by this entry. */
    private final DocumentModelBridge document;

    /** Used for resolving the content author from the document. */
    private final ContentAuthorResolver contentAuthorResolver;

    /**
     * @param document {@see document}
     * @param contentAuthorResolver {@see contentAuthorResolver}
     */
    public DocumentSecurityStackEntry(DocumentModelBridge document, ContentAuthorResolver contentAuthorResolver)
    {
        this.document = document;
        this.contentAuthorResolver = contentAuthorResolver;
    }

    @Override
    public boolean grantProgrammingRight()
    {
        return false;
    }

    @Override
    public DocumentReference getContentAuthor()
    {
        return contentAuthorResolver.resolveContentAuthor(document);
    }

}
