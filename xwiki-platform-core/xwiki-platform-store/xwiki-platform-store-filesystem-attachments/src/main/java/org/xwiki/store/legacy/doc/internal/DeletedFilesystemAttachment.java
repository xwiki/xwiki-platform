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
package org.xwiki.store.legacy.doc.internal;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;

/**
 * Filesystem based Archive of deleted attachment, stored in
 * {@link org.xwiki.store.legacy.store.internal.FilesystemAttachmentRecycleBinStore}.
 *
 * @version $Id$
 * @since 3.0M3
 */
public class DeletedFilesystemAttachment extends DeletedAttachment
{
    /**
     * The attachment which was deleted.
     */
    private XWikiAttachment attachment;

    /**
     * Protected Constructor. Used by MutableDeletedFilesystemAttachment.
     */
    protected DeletedFilesystemAttachment()
    {
    }

    /**
     * A constructor with all the information about the deleted attachment.
     *
     * @param attachment Deleted attachment.
     * @param deleter User which deleted the attachment.
     * @param deleteDate Date of delete action.
     * @throws XWikiException is never thrown, we just have to declare it in order to call the super constructor
     */
    public DeletedFilesystemAttachment(final XWikiAttachment attachment, final String deleter, final Date deleteDate)
        throws XWikiException
    {
        super(attachment, deleter, deleteDate, null);
    }

    @Override
    public long getDocId()
    {
        // TODO deprecate me.
        if (this.attachment != null && this.attachment.getDoc() != null) {
            return this.attachment.getDocId();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The XWiki context is unused and may safely be null.
     *
     * @see com.xpn.xwiki.doc.DeletedAttachment#setAttachment(XWikiAttachment, XWikiContext)
     */
    @Override
    protected void setAttachment(final XWikiAttachment attachment, final XWikiContext context)
    {
        this.attachment = (XWikiAttachment) attachment.clone();
        if (this.getDate() != null) {
            this.setId(generateId(this.attachment, this.getDate()));
        }
    }

    @Override
    public void setDate(Date date)
    {
        super.setDate(date);
        if (this.getAttachment() != null) {
            this.setId(generateId(this.getAttachment(), date));
        }
    }

    /**
     * Get the attachment. This does not clone the attachment. To get a clone, use {@link
     * #restoreAttachment(XWikiAttachment, XWikiContext)}
     *
     * @return the attachment which was deleted.
     */
    public XWikiAttachment getAttachment()
    {
        return this.attachment;
    }

    @Override
    public XWikiAttachment restoreAttachment(final XWikiAttachment attachment, final XWikiContext context)
        throws XWikiException
    {
        XWikiAttachment result = attachment;
        if (result != null) {
            // TODO Add XWikiAttachment#clone(XWikiAttachment)
            // this toXML does not copy content.
            result.fromXML(this.attachment.toXML(context));
            if (this.attachment.getAttachment_content() != null) {
                attachment.setAttachment_content((XWikiAttachmentContent) this.attachment.getAttachment_content()
                    .clone());
                attachment.getAttachment_content().setAttachment(attachment);
            }
            if (this.attachment.getAttachment_archive() != null) {
                result.setAttachment_archive((XWikiAttachmentArchive) this.attachment.getAttachment_archive().clone());
                result.getAttachment_archive().setAttachment(result);
            }
        } else {
            result = (XWikiAttachment) this.attachment.clone();
        }

        result.setDoc(context.getWiki().getDocument(this.attachment.getReference().getDocumentReference(), context));
        return result;
    }

    /**
     * Generate an ID which will be as collision resistant as possible. Because {@link
     * com.xpn.xwiki.doc.XWikiAttachment#getId()} returns an int cast to a long, this ID is guaranteed to be unique
     * unless the same attachment is deleted twice in the same second or again in a second which will come around in
     * another 136 years.
     *
     * @param attachment the attachment to get an ID number for.
     * @param deleteDate the Date the attachment was deleted.
     * @return an ID number for this deleted attachment.
     */
    private static long generateId(final XWikiAttachment attachment, final Date deleteDate)
    {
        return (attachment.getId() << 32) ^ ((deleteDate.getTime() / 1000) & 0x00000000FFFFFFFFL);
    }
}
