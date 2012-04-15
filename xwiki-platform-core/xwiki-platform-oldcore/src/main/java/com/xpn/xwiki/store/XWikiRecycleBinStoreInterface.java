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

import java.util.Date;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface for RecycleBin feature (XWIKI-543) store system.
 * 
 * @version $Id$
 * @since 1.2M1
 */
@Role
public interface XWikiRecycleBinStoreInterface
{
    /**
     * Save document to recycle bin.
     * 
     * @param doc - document to save
     * @param deleter - the user which delete document
     * @param date - date of delete action
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @param context - used while saving
     * @throws XWikiException if error in saving
     */
    void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @return restored document from recycle bin
     * @param doc - document to restore
     * @param index - what deleted document to restore. see {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if error while loading
     */
    XWikiDocument restoreFromRecycleBin(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @return specified deleted document from recycle bin. null if not found.
     * @param doc - deleted document
     * @param index - what deleted document to restore. see {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if error while loading
     */
    XWikiDeletedDocument getDeletedDocument(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @return infos about all delete actions of specific document. sorted by date.
     * @param doc - the deleted document
     * @param context - used to load
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException - if error in loading
     */
    XWikiDeletedDocument[] getAllDeletedDocuments(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Permanently delete document from recycle bin.
     * 
     * @param doc - document to delete
     * @param index - which instance document in recycle bin to delete
     * @param context - used for environment
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if eny error
     */
    void deleteFromRecycleBin(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;
}
