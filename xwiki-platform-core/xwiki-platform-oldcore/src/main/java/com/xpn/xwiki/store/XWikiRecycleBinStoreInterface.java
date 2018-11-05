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
     * Save document to recycle bin.
     *
     * @param doc - document to save
     * @param deleter - the user which delete document
     * @param date - date of delete action
     * @param batchId - id of the operation that deleted multiple documents at the same time, useful when trying to
     *            revert the operation. {@code null} or empty values are ignored
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @param context - used while saving
     * @throws XWikiException if error in saving
     * @since 9.4RC1
     */
    default void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, String batchId, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        // XXX: The current signature does not return the saved document index so we have no way of setting the batchId
        // to the save document. This means we can`t completely respect the method`s contract, but at least the most
        // important part of the work is done and the document can be individually restored.
        saveToRecycleBin(doc, deleter, date, context, bTransaction);
    }

    /**
     * @return restored document from recycle bin
     * @param doc - document to restore
     * @param index - what deleted document to restore. see {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if error while loading
     * @deprecated since 9.4RC1. The document parameter is useless and gets in the way. Use
     *             {@link #restoreFromRecycleBin(long, XWikiContext, boolean)} instead.
     */
    @Deprecated
    XWikiDocument restoreFromRecycleBin(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @param index - what deleted document to restore. see {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @return the restored document from recycle bin
     * @throws XWikiException if error while loading
     * @since 9.4RC1
     */
    default XWikiDocument restoreFromRecycleBin(long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        // XXX: Depending on how an older implementation handled the XWikiDocument argument, it's relatively safer to
        // pass an empty document than null. However, if the document's reference is actually used, the result might be
        // unpredictable.
        return restoreFromRecycleBin(new XWikiDocument(), index, context, bTransaction);
    }

    /**
     * @return specified deleted document from recycle bin. null if not found.
     * @param doc - deleted document
     * @param index - what deleted document to restore. see {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if error while loading
     * @deprecated since 9.4RC1. The document parameter is useless and gets in the way. Use
     *             {@link #getDeletedDocument(long, XWikiContext, boolean)} instead.
     */
    @Deprecated
    XWikiDeletedDocument getDeletedDocument(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @param index - what deleted document to restore. See {@link XWikiDeletedDocument#getId()}
     * @param context - used while loading
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @return specified deleted document from recycle bin or {@code null} if not found.
     * @throws XWikiException if error while loading
     * @since 9.4RC1
     */
    default XWikiDeletedDocument getDeletedDocument(long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        // XXX: Depending on how an older implementation handled the XWikiDocument argument, it's relatively safer to
        // pass an empty document than null. However, if the document's reference is actually used, the result might be
        // unpredictable.
        return getDeletedDocument(new XWikiDocument(), index, context, bTransaction);
    }

    /**
     * @return info about all delete actions of specific document. sorted by date.
     * @param doc - the deleted document
     * @param context - used to load
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException - if error in loading
     */
    XWikiDeletedDocument[] getAllDeletedDocuments(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Get all the deleted documents ID or a specified number. Sorted by date.
     * @param context - used to load the deleted documents id.
     * @param limit - if > 0 then all deleted documents id are returned. Else the specified number.
     * @return an array of IDs of deleted documents.
     * @throws XWikiException - if error in loading
     * @since 10.10RC1
     */
    default Long[] getAllDeletedDocumentsIds(XWikiContext context, int limit) throws XWikiException
    {
        return new Long[0];
    }

    /**
     * @param context - used to realize the query.
     * @return the number of deleted documents in the recycle bin.
     * @throws XWikiException - if error in loading.
     * @since 10.10RC1
     */
    default int getNumberOfDeletedDocuments(XWikiContext context) throws XWikiException
    {
        return -1;
    }

    /**
     * @return info about all documents that were deleted in the same batch, as part of the same operation
     * @param batchId - id of the operation that deleted multiple documents at the same time; useful when trying to
     *            revert the operation
     * @param context - used to load
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException - if error in loading
     * @since 9.4RC1
     */
    default XWikiDeletedDocument[] getAllDeletedDocuments(String batchId, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        // Return no results as default implementation.
        return new XWikiDeletedDocument[0];
    }

    /**
     * @param batchId - id of the operation that deleted multiple documents at the same time; useful when trying to
     *            revert the operation
     * @param withContent - {@code true} if the deleted document's content should also be loaded; {@code false} if
     * @param context - used to load
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @return info about all documents that were deleted in the same batch, as part of the same operation
     * @throws XWikiException - if error in loading
     * @since 9.4RC1
     */
    default XWikiDeletedDocument[] getAllDeletedDocuments(String batchId, boolean withContent, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        // Return no results as default implementation.
        return new XWikiDeletedDocument[0];
    }

    /**
     * Permanently delete document from recycle bin.
     *
     * @param doc - document to delete
     * @param index - which instance document in recycle bin to delete
     * @param context - used for environment
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if any error
     * @deprecated since 9.4RC1. The document parameter is useless and gets in the way. Use
     *             {@link #deleteFromRecycleBin(long, XWikiContext, boolean)} instead.
     */
    @Deprecated
    void deleteFromRecycleBin(XWikiDocument doc, long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Permanently delete document from recycle bin.
     *
     * @param index - which instance document in recycle bin to delete
     * @param context - used for environment
     * @param bTransaction - should use old transaction(false) or create new (true)
     * @throws XWikiException if any error
     * @since 9.4RC1
     */
    default void deleteFromRecycleBin(long index, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        // XXX: Depending on how an older implementation handled the XWikiDocument argument, it's relatively safer to
        // pass an empty document than null. However, if the document's reference is actually used, the result might be
        // unpredictable.
        deleteFromRecycleBin(new XWikiDocument(), index, context, bTransaction);
    }
}
