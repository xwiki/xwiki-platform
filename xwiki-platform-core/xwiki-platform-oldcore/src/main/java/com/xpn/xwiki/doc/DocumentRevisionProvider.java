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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Provide generic way of requesting a specific revision of a document from several sources (database, installed XAR
 * extension, etc.).
 * <p>
 * The revision syntax is {@code <provider hint>:<revision>}, default provide being database (the version of the
 * document in the history).
 * 
 * @version $Id$
 * @since 9.4RC1
 */
@Role
public interface DocumentRevisionProvider
{
    /**
     * Load the document in the provided revision.
     * 
     * @param reference the reference of the document
     * @param revision the revision of the document
     * @return the {@link XWikiDocument} instance or null if none existing
     * @throws XWikiException when failing to load the document revision
     */
    XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException;

    /**
     * Load the document in the provided revision.
     * 
     * @param document the current document
     * @param revision the revision of the document
     * @return the {@link XWikiDocument} instance or null if none existing
     * @throws XWikiException when failing to load the document revision
     */
    XWikiDocument getRevision(XWikiDocument document, String revision) throws XWikiException;
}
