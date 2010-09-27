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
import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface for AttachmentRecycleBin feature (XWIKI-2254) store system. Attachments can be placed in the recycle bin
 * using {@link #saveToRecycleBin(XWikiAttachment, String, Date, XWikiContext, boolean)}, restored using
 * {@link #restoreFromRecycleBin(XWikiAttachment, long, XWikiContext, boolean)}, and permanently removed from the
 * recycle bin using {@link #deleteFromRecycleBin(XWikiAttachment, long, XWikiContext, boolean)}.
 * 
 * @version $Id$
 * @since 1.4M1
 */
@ComponentRole
public interface AttachmentRecycleBinStore
{
    /**
     * Save attachment to recycle bin, with full history.
     * 
     * @param attachment The attachment to save.
     * @param deleter The user which deleted the attachment.
     * @param date Date of delete action.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an exception occurs during the attachment export or attachment persistence.
     */
    void saveToRecycleBin(XWikiAttachment attachment, String deleter, Date date, XWikiContext context,
        boolean bTransaction) throws XWikiException;

    /**
     * Restore an attachment from the recycle bin (with full history).
     * 
     * @return Restored attachment, or <code>null</code> if an entry with the requested ID does not exist.
     * @param attachment Optional attachment to restore. If a non-null value is passed, then this object will be changed
     *            to reflect the contents/history of the deleted attachment.
     * @param index What deleted attachment to restore. See {@link DeletedAttachment#getId()}.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs while loading or restoring the attachment.
     * @see #getDeletedAttachment(XWikiAttachment, long, XWikiContext, boolean)
     */
    XWikiAttachment restoreFromRecycleBin(XWikiAttachment attachment, long index, XWikiContext context,
        boolean bTransaction) throws XWikiException;

    /**
     * Returns a {@link DeletedAttachment handler} for a deleted attachment.
     * 
     * @return Specified deleted document from recycle bin. <code>null</code> if not found.
     * @param index What deleted attachment to restore. See {@link DeletedAttachment#getId()}
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs while loading or restoring the attachment.
     * @see #restoreFromRecycleBin(XWikiAttachment, long, XWikiContext, boolean)
     */
    DeletedAttachment getDeletedAttachment(long index, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Get all the deleted attachments from the database matching an attachment template (document name and filename).
     * The results are ordered by the deletion date, descending (most recently deleted first).
     * 
     * @param attachment Optional attachment template. If <code>null</code>, return information about all deleted
     *            attachments from the database. Otherwise, filter by the document and filename provided in the passed
     *            attachment.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @return Infos about all matching deleted attachments, sorted by date.
     * @throws XWikiException If an error occurs while loading or restoring the attachments.
     */
    List<DeletedAttachment> getAllDeletedAttachments(XWikiAttachment attachment, XWikiContext context,
        boolean bTransaction) throws XWikiException;

    /**
     * Get all the deleted attachments for a given document.
     * 
     * @return Infos about all deleted attachments of specific document, sorted by date.
     * @param doc The document for which to retrieve deleted attachments.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs while loading or restoring the attachments.
     */
    List<DeletedAttachment> getAllDeletedAttachments(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Permanently delete attachment from recycle bin.
     * 
     * @param index Which instance to delete from the recycle bin.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs while executing the query.
     */
    void deleteFromRecycleBin(long index, XWikiContext context, boolean bTransaction) throws XWikiException;
}
