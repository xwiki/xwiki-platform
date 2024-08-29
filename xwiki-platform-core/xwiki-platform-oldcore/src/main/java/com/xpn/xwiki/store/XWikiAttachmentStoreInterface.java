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

import java.util.List;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Interface for Attachment store system.
 *
 * @version $Id$
 */
@Role
public interface XWikiAttachmentStoreInterface
{
    /**
     * @return the role hint of the component
     * @since 9.10RC1
     */
    String getHint();

    void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, XWikiContext context,
        boolean bTransaction) throws XWikiException;

    void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiDocument doc, boolean bParentUpdate,
        XWikiContext context, boolean bTransaction) throws XWikiException;

    void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    /**
     * @param attachment
     * @param context the XWiki context
     * @param bTransaction
     * @return true of the content of this attachment still exist in the store
     * @throws XWikiException when it's not possible to check of the content exist
     * @since 13.8RC1
     * @since 13.4.4
     * @since 12.10.10
     */
    default boolean attachmentContentExists(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return true;
    }

    void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException;

    void deleteXWikiAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context,
        boolean bTransaction) throws XWikiException;

    void cleanUp(XWikiContext context);
}
