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
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.util.Programming;
import com.xpn.xwiki.web.Utils;

/**
 * Information about a deleted document in the recycle bin.
 *
 * @version $Id$
 */
public class DeletedDocument extends Api
{
    private static final String ADMIN_RIGHT = "admin";

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletedDocument.class);

    /**
     * The internal object wrapped by this API.
     */
    private final XWikiDeletedDocument deletedDoc;

    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    /**
     * Simple constructor, initializes a new API object with the current {@link com.xpn.xwiki.XWikiContext context} and
     * the specified protected {@link com.xpn.xwiki.doc.XWikiDeletedDocument deleted document} object.
     *
     * @param deletedDoc the internal object wrapped by this API
     * @param context the current request context
     */
    public DeletedDocument(XWikiDeletedDocument deletedDoc, XWikiContext context)
    {
        super(context);
        this.deletedDoc = deletedDoc;
    }

    /**
     * @return full name of document (ie: Main.WebHome)
     */
    public String getFullName()
    {
        return this.deletedDoc.getFullName();
    }

    /**
     * @return locale of document
     * @deprecated since 8.0M1, use {@link #getLocale()} instead
     */
    @Deprecated
    public String getLanguage()
    {
        return this.deletedDoc.getLanguage();
    }

    /**
     * @return locale of document
     * @since 8.0M1
     */
    public Locale getLocale()
    {
        return this.deletedDoc.getLocale();
    }

    /**
     * @return date of delete action
     */
    public Date getDate()
    {
        return this.deletedDoc.getDate();
    }

    /**
     * @return user which delete document
     */
    public String getDeleter()
    {
        return this.deletedDoc.getDeleter();
    }

    /**
     * @return the reference of the user who deleted this document
     * @since 11.5RC1
     */
    public DocumentReference getDeleterReference()
    {
        return this.deletedDoc.getDeleterReference();
    }

    /**
     * @return id of deleted document. id is unique only for this document.
     */
    public long getId()
    {
        return this.deletedDoc.getId();
    }

    /**
     * @return the id of the operation that deleted multiple documents at the same time, including this one
     * @since 9.4RC1
     */
    public String getBatchId()
    {
        return this.deletedDoc.getBatchId();
    }

    private UserReferenceResolver<DocumentReference> getUserReferenceResolver()
    {
        if (this.userReferenceResolver == null) {
            this.userReferenceResolver = Utils.getComponent(
                new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class), "document");
        }
        return this.userReferenceResolver;
    }

    private boolean hasAccess(Right right)
    {
        UserReference userReference = getUserReferenceResolver().resolve(this.context.getUserReference());
        XWikiRecycleBinStoreInterface recycleBinStore = this.context.getWiki().getRecycleBinStore();
        return recycleBinStore.hasAccess(right, userReference, this.deletedDoc);
    }

    /**
     * Check if the current user has the right to restore the document.
     *
     * @return {@code true} if the current user can restore this document, {@code false} otherwise
     */
    public boolean canUndelete()
    {
        return hasAccess(Right.EDIT);
    }

    /**
     * Check if the current user has the right to view the deleted document.
     * This is allowed either if the user has admin right on the original reference of the doc, or if they were the
     * original user who deleted it.
     *
     * @return {code true} if the current user is allowed to view the deleted document.
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    public boolean canView()
    {
        return hasAccess(Right.VIEW);
    }

    /**
     * @return {@code true} if the current user can permanently delete this document, {@code false} otherwise
     * @xwiki.xwikicfg xwiki.store.recyclebin.adminWaitDays How many days should an administrator wait before being able
     *                 to permanently delete this document from the recycle bin. 0 by default.
     * @xwiki.xwikicfg xwiki.store.recyclebin.waitDays How many days should a normal user with "delete" right wait
     *                 before being able to permanently delete this document from the recycle bin. 7 by default.
     */
    public boolean canDelete()
    {
        try {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(getFullName(), this.context);
            if (!hasAccessLevel("delete", getFullName())) {
                return false;
            }
            String waitdays;
            if (hasAccessLevel(ADMIN_RIGHT, getFullName())) {
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
            LOGGER.warn("Exception while checking if entry [{}] can be removed from the recycle bin", getId(), ex);
            return false;
        }
    }

    /**
     * @return original deleted document if user has programming rights, else {@code null}.
     */
    @Programming
    public XWikiDeletedDocument getDeletedDocument()
    {
        if (hasProgrammingRights()) {
            return this.deletedDoc;
        } else {
            return null;
        }
    }

    /**
     * @return the document as it is in the recycle bin if the user is allowed to restore it, {@code null} otherwise
     */
    public Document getDocument()
    {
        if (canUndelete()) {
            try {
                return new Document(this.deletedDoc.restoreDocument(null, this.context), this.context);
            } catch (XWikiException e) {
                LOGGER.warn("Failed to parse deleted document [{}]", getFullName(), e);
            }
        }

        return null;
    }

    /**
     * @return the document reference for the deleted document, including any locale information
     * @since 9.4RC1
     */
    public DocumentReference getDocumentReference()
    {
        return this.deletedDoc.getDocumentReference();
    }
}
