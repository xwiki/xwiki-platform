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
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Store the content of a deleted attachment.
 * 
 * @version $Id$
 * @since 9.9RC1
 */
@Role
public interface AttachmentRecycleBinContentStore
{
    /**
     * @return the hint of the component
     */
    String getHint();

    /**
     * Save attachment to recycle bin.
     *
     * @param attachment the attachment to save
     * @param deleteDate the date of the delete
     * @param index the index of the deleted attachment
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @throws XWikiException if error in saving
     */
    void save(XWikiAttachment attachment, Date deleteDate, long index, boolean bTransaction) throws XWikiException;

    /**
     * @param reference the reference of the deleted attachment
     * @param deleteDate the date of the delete
     * @param index the index of the deleted attachment
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @return specified deleted attachment from recycle bin. null if not found.
     * @throws XWikiException if error while loading
     */
    DeletedAttachmentContent get(AttachmentReference reference, Date deleteDate, long index, boolean bTransaction)
        throws XWikiException;

    /**
     * Permanently delete attachment content from recycle bin.
     *
     * @param reference the reference of the deleted attachment
     * @param deleteDate the date of the delete
     * @param index the index of the deleted attachment
     * @param bTransaction indicate if the store should use old transaction(false) or create new (true)
     * @throws XWikiException if any error
     */
    void delete(AttachmentReference reference, Date deleteDate, long index, boolean bTransaction) throws XWikiException;
}
