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
package com.xpn.xwiki.api;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Programming;

/**
 * Information about a deleted attachment in the recycle bin. Note that this does not hold much information about the
 * real attachment, but only meta-information relevant to the trash: original document and filename, deleter, deletion
 * date. The attachment can be accessed using {@link #getAttachment()}.
 * <p>
 * This object is immutable, since entries in the trash can not be modified.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class DeletedAttachment extends Api
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletedAttachment.class);

    /** The internal object wrapped by this API. */
    private final com.xpn.xwiki.doc.DeletedAttachment deletedAttachment;

    /**
     * Simple constructor, initializes a new API object with the current {@link com.xpn.xwiki.XWikiContext context} and
     * the specified protected {@link com.xpn.xwiki.doc.DeletedAttachment deleted attachment} object.
     *
     * @param deletedAttachment the internal object wrapped by this API
     * @param context the current request context
     */
    public DeletedAttachment(com.xpn.xwiki.doc.DeletedAttachment deletedAttachment, XWikiContext context)
    {
        super(context);
        this.deletedAttachment = deletedAttachment;
    }

    /**
     * Retrieve the internal entry index, used to uniquely identify this entity in the trash. This is needed because a
     * file can be attached and deleted multiple times, so the document name and filename are not enough to uniquely
     * identify a deleted attachment.
     *
     * @return internal identifier of the corresponding trash entry
     */
    public long getId()
    {
        return this.deletedAttachment.getId();
    }

    /**
     * Retrieve the original name of this attachment.
     *
     * @return the original filename, for example {@code MyPhoto.png}
     */
    public String getFilename()
    {
        return this.deletedAttachment.getFilename();
    }

    /**
     * Retrieve the name of the document this attachment belonged to.
     *
     * @return the name of the owner document, in the {@code Space.Document} format
     */
    public String getDocName()
    {
        return this.deletedAttachment.getDocName();
    }

    /**
     * Retrieve the name of the user who deleted this attachment.
     *
     * @return the user who deleted the attachment, as its document name (e.g. {@code XWiki.Admin})
     */
    public String getDeleter()
    {
        return this.deletedAttachment.getDeleter();
    }

    /**
     * Retrieve the date and time this attachment has been deleted.
     *
     * @return the date of the deletion
     */
    public Date getDate()
    {
        return this.deletedAttachment.getDate();
    }

    /**
     * Access to the real attachment object.
     *
     * @return the attachment as it was before being deleted, and as it currently is in the recycle bin
     */
    public Attachment getAttachment()
    {
        try {
            XWikiAttachment attachment = this.deletedAttachment.restoreAttachment();

            if (attachment != null) {
                Document doc = this.context.getWiki().getDocument(getDocName(), this.context).newDocument(this.context);

                return new Attachment(doc, attachment, this.context);
            }
        } catch (XWikiException ex) {
            LOGGER.warn("Failed to parse deleted attachment", ex);
        }

        return null;
    }

    /**
     * Privileged access to the internal object wrapped by this API.
     *
     * @return original deleted attachment if the current user has programming rights, else {@code null}.
     */
    @Programming
    public com.xpn.xwiki.doc.DeletedAttachment getDeletedAttachment()
    {
        if (hasProgrammingRights()) {
            return this.deletedAttachment;
        } else {
            return null;
        }
    }

    /**
     * Check if the current user has the right to restore the attachment.
     *
     * @return {@code true} if the current user can restore this document, {@code false} otherwise
     */
    public boolean canRestore()
    {
        // FIXME Temporary disabled until this action is implemented.
        // As a workaround, the attachment can be downloaded and re-attached.
        return false;
    }

    /**
     * Check if the current user has the right to permanently delete the attachment from the trash.
     *
     * @return {@code true} if the current user can purge this document, {@code false} otherwise
     * @xwiki.xwikicfg xwiki.store.recyclebin.adminWaitDays How many days should an administrator wait before being able
     *                 to permanently delete this document from the recycle bin. 0 by default.
     * @xwiki.xwikicfg xwiki.store.recyclebin.waitDays How many days should a normal user with "delete" right wait
     *                 before being able to permanently delete this document from the recycle bin. 7 by default.
     */
    public boolean canDelete()
    {
        try {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(getDocName(), this.context);
            if (!hasAdminRights()
                && !getXWikiContext().getWiki().getRightService().checkAccess("delete", doc, this.context)) {
                return false;
            }
            String waitdays;
            if (hasAdminRights()) {
                waitdays = getXWikiContext().getWiki().Param("xwiki.store.recyclebin.adminWaitDays", "0");
            } else {
                waitdays = getXWikiContext().getWiki().Param("xwiki.store.recyclebin.waitDays", "7");
            }
            int seconds = (int) (Double.parseDouble(waitdays) * 24 * 60 * 60 + 0.5);
            Calendar cal = Calendar.getInstance();
            cal.setTime(getDate());
            cal.add(Calendar.SECOND, seconds);
            return cal.before(Calendar.getInstance());
        } catch (Exception ex) {
            // Public APIs should not throw exceptions
            LOGGER.warn("Exception while checking if entry [" + getId() + "] can be removed from the recycle bin", ex);
            return false;
        }
    }

    /**
     * Permanently delete this attachment from the trash. Throws an access denied exception if the user does not have
     * the right to perform this action, which will trigger the generic Access Denied message. Any other failures will
     * be silently ignored.
     *
     * @throws XWikiException if the user does not have the right to perform this action
     */
    public void delete() throws XWikiException
    {
        if (this.canDelete()) {
            try {
                this.context.getWiki().getAttachmentRecycleBinStore().deleteFromRecycleBin(getId(), this.context, true);
            } catch (Exception ex) {
                LOGGER.warn("Failed to purge deleted attachment", ex);
            }
        } else {
            java.lang.Object[] args = { this.getFilename(), this.getDocName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Cannot permanently delete attachment {0}@{1} from the trash", null, args);
        }
    }
}
