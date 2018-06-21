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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.AuthorizationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for all API Objects. API Objects are the Java Objects that can be manipulated from Velocity or Groovy in
 * XWiki documents.
 *
 * @version $Id$
 */
public class Api
{
    /**
     * The current context, needed by the underlying protected object.
     *
     * @see #getXWikiContext()
     */
    protected XWikiContext context;

    private Provider<XWikiContext> xcontextProvider;

    private AuthorizationManager authorizationManager;

    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * @param context the XWiki Context object
     * @see #getXWikiContext()
     */
    public Api(XWikiContext context)
    {
        this.context = context;
    }

    /**
     * Note 1: This method is protected so that users of this API do not get to see the XWikiContext object which should
     * not be exposed.
     * <p>
     * Note 2: This is not longer the canonical way of retrieving the XWiki Context. The new way is to get it from the
     * {@link org.xwiki.context.ExecutionContext}.
     *
     * @return the current context containing all state information about the current request
     */
    protected XWikiContext getXWikiContext()
    {
        if (this.xcontextProvider == null) {
            this.xcontextProvider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);
        }

        // TODO: We need to get rid of this.context but since it's been protected for a long time, a lot of code
        // wrongly use it instead of calling this getXWikiContext() method. Thus the best we can do ATM is to sync
        // the saved context with the dynamic one we just retrieved...
        this.context = this.xcontextProvider.get();

        return this.context;
    }

    /**
     * @return the {@link AuthorizationManager} component
     * @since 10.6RC1
     */
    protected AuthorizationManager getAuthorizationManager()
    {
        if (this.authorizationManager == null) {
            this.authorizationManager = Utils.getComponent(AuthorizationManager.class);
        }

        return this.authorizationManager;
    }

    private EntityReferenceSerializer<String> getDefaultEntityReferenceSerializer()
    {
        if (this.defaultEntityReferenceSerializer == null) {
            this.defaultEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        }

        return this.defaultEntityReferenceSerializer;
    }

    /**
     * Check if the current document has programming rights, meaning that it was last saved by a user with the
     * programming right globally granted.
     *
     * @return <tt>true</tt> if the current document has the Programming right or <tt>false</tt> otherwise.
     */
    public boolean hasProgrammingRights()
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasProgrammingRights(this.context);
    }

    /**
     * Check if the current user has administration rights either on the current wiki or on the current space.
     *
     * @return <code>true</code> if the current user has the <code>admin</code> right or <code>false</code> otherwise.
     */
    public boolean hasAdminRights()
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasAdminRights(this.context);
    }

    /**
     * Check if the current user has administration rights on the current wiki, regardless of any space admin rights
     * that might also be available.
     *
     * @return <code>true</code> if the current user has the <code>admin</code> right or <code>false</code> otherwise.
     * @since 3.2M3
     */
    public boolean hasWikiAdminRights()
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasWikiAdminRights(this.context);
    }

    /**
     * Check if the current user has an access level on a given document.
     *
     * @param right The name of the right to verify (eg "programming", "admin", "register", etc).
     * @param docname The document for which to verify the right.
     * @return <tt>true</tt> if the current user has the specified right, <tt>false</tt> otherwise.
     * @exception XWikiException In case of an error finding the document or accessing groups information.
     */
    public boolean hasAccessLevel(String right, String docname) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = this.context.getWiki();
        return xwiki.getRightService().hasAccessLevel(right, this.context.getUser(), docname, this.context);
    }

    /**
     * Convert a list of internal representation of documents to public api documents.
     *
     * @param xdocList the internal documents.
     * @return the plublic api documents.
     */
    protected List<Document> convert(List<XWikiDocument> xdocList)
    {
        List<Document> docList = new ArrayList<Document>();
        for (XWikiDocument xdoc : xdocList) {
            docList.add(xdoc.newDocument(this.context));
        }

        return docList;
    }

    /**
     * Convert an internal representation of document to public api document.
     *
     * @param xdoc the internal document.
     * @return the plublic api document.
     */
    protected Document convert(XWikiDocument xdoc)
    {
        return xdoc == null ? null : xdoc.newDocument(this.context);
    }

    /**
     * Get the name of the content author of the current document for security checking. If
     * {@link XWikicontext#dropPermissions()} has been called then this will return the guest user no matter who the
     * real author is. If there is no current document then the guest user is returned because there is no reason for
     * script to have any permission if does not exist in any document.
     *
     * @return the name of the document author or guest.
     * @deprecated use {@link #getEffectiveAuthorReference()} instead
     */
    @Deprecated
    String getEffectiveScriptAuthorName()
    {
        DocumentReference authorReference = getXWikiContext().getAuthorReference();

        return authorReference != null ? getDefaultEntityReferenceSerializer().serialize(authorReference)
            : XWikiRightService.GUEST_USER;
    }

    /**
     * Get the reference of the current author for security checking. If {@link XWikicontext#dropPermissions()} has been
     * called then this will return the guest user no matter who the real author is.
     *
     * @return the name of the document author or guest.
     */
    DocumentReference getEffectiveAuthorReference()
    {
        if (!this.getXWikiContext().hasDroppedPermissions()) {
            return getXWikiContext().getAuthorReference();
        }

        return null;
    }

    /**
     * Convert an internal representation of an attachment to the public api Attachment.
     *
     * @param xattach The internal XWikiAttachment object
     * @return The public api Attachment object
     * @since 5.0M2
     */
    protected Attachment convert(XWikiAttachment xattach)
    {
        return xattach == null ? null : new Attachment(convert(xattach.getDoc()), xattach, this.context);
    }

    /**
     * Convert a list of attachments in their internal form to a list of public api Attachments.
     *
     * @param xattaches The List of XWikiAttachment objects
     * @return A List of Attachment objects
     * @since 5.0M2
     */
    protected List<Attachment> convertAttachments(List<XWikiAttachment> xattaches)
    {
        List<Attachment> outList = new ArrayList<Attachment>(xattaches.size());
        for (XWikiAttachment xattach : xattaches) {
            outList.add(convert(xattach));
        }
        return outList;
    }
}
