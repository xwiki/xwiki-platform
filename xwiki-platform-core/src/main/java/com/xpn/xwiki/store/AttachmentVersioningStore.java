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

import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;

/**
 * Interface for storing attachment versions.
 * 
 * @version $Id$
 * @since 1.4M2
 */
@ComponentRole
public interface AttachmentVersioningStore
{
    /**
     * Load attachment archive from store.
     * 
     * @return attachment archive. not null. return empty archive if it is not exist in store.
     * @param attachment The attachment of archive.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs.
     */
    XWikiAttachmentArchive loadArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * Save or update attachment archive.
     * 
     * @param archive The attachment archive to save.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs.
     */
    void saveArchive(XWikiAttachmentArchive archive, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * Permanently delete attachment archive.
     * 
     * @param attachment The attachment to delete.
     * @param context The current context.
     * @param bTransaction Should use old transaction (false) or create new (true).
     * @throws XWikiException If an error occurs.
     */
    void deleteArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
}
