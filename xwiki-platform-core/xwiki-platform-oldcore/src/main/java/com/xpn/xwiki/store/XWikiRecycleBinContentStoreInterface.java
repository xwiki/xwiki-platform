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
package com.xpn.xwiki.store;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Store the content of a deleted document.
 *
 * @version $Id$
 * @since 9.0RC1
 */
@Role
public interface XWikiRecycleBinContentStoreInterface
{
    /**
     * @return the hint of the component
     */
    String getHint();

    /**
     * Save document to recycle bin.
     *
     * @param doc the document to save
     * @param index the index of the deleted document
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @throws XWikiException if error in saving
     */
    void save(XWikiDocument doc, long index, boolean bTransaction) throws XWikiException;

    /**
     * @param reference the reference of the deleted document
     * @param index the index of the deleted document
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @return specified deleted document from recycle bin. null if not found.
     * @throws XWikiException if error while loading
     */
    XWikiDeletedDocumentContent get(DocumentReference reference, long index, boolean bTransaction)
        throws XWikiException;

    /**
     * Permanently delete document content from recycle bin.
     *
     * @param reference the reference of the deleted document
     * @param index the index of the deleted document
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @throws XWikiException if any error
     */
    void delete(DocumentReference reference, long index, boolean bTransaction) throws XWikiException;
}
