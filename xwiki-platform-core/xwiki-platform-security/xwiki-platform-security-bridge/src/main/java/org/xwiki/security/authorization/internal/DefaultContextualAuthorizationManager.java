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
package org.xwiki.security.authorization.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of the {@link ContextualAuthorizationManager}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Singleton
public class DefaultContextualAuthorizationManager implements ContextualAuthorizationManager
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void checkAccess(Right right) throws AccessDeniedException
    {
        if (right == Right.PROGRAM) {
            checkAccess(right, getCurrentUser(right, null), null);
        }

        checkAccess(right, right == Right.PROGRAM ? null : getCurrentEntity());
    }

    @Override
    public void checkAccess(Right right, EntityReference entity) throws AccessDeniedException
    {
        DocumentReference user = getCurrentUser(right, entity);

        checkAccess(right, user, entity);
    }

    private void checkAccess(Right right, DocumentReference user, EntityReference entity) throws AccessDeniedException
    {
        if (!checkPreAccess(right)) {
            throw new AccessDeniedException(right, user, entity);
        }

        this.authorizationManager.checkAccess(right, user, entity);
    }

    @Override
    public boolean hasAccess(Right right)
    {
        if (right == Right.PROGRAM) {
            return hasAccess(right, getCurrentUser(right, null), null);
        }

        return hasAccess(right, getCurrentEntity());
    }

    @Override
    public boolean hasAccess(Right right, EntityReference entity)
    {
        DocumentReference user = getCurrentUser(right, entity);

        return hasAccess(right, user, entity);
    }

    private boolean hasAccess(Right right, DocumentReference user, EntityReference entity)
    {
        return checkPreAccess(right) && this.authorizationManager.hasAccess(right, user, entity);
    }

    /**
     * Check pre-condition for access.
     *
     * @param right the right being checked.
     * @return true if pre-condition are fulfilled.
     */
    private boolean checkPreAccess(Right right)
    {
        if (right == Right.PROGRAM) {
            if (this.renderingContext.isRestricted() || this.xcontextProvider.get().hasDroppedPermissions()) {
                return false;
            }
        }

        return true;
    }

    private DocumentReference getCurrentUser(Right right, EntityReference entity)
    {
        // Backward compatibility for the old way of assigning programming right.
        if (right == Right.PROGRAM) {
            XWikiDocument doc = entity == null ? getProgrammingDocument() : getDocument(entity);
            if (doc != null) {
                return getContentAuthor(doc);
            }
        }

        return this.xcontextProvider.get().getUserReference();
    }

    private XWikiDocument getDocument(EntityReference entity)
    {
        if (entity == null) {
            return null;
        }

        EntityReference docEntity = entity.extractReference(EntityType.DOCUMENT);
        if (docEntity == null) {
            return null;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            return xcontext.getWiki().getDocument(new DocumentReference(docEntity), xcontext);
        } catch (Exception e) {
            // Ignored
        }

        return null;
    }

    /**
     * @param doc a document.
     * @return the content author reference of that document.
     */
    private DocumentReference getContentAuthor(XWikiDocument doc)
    {
        DocumentReference user = doc.getContentAuthorReference();

        if (user != null && XWikiConstants.GUEST_USER.equals(user.getName())) {
            // Public users (not logged in) should be passed as null in the new API. It may happen that badly
            // design code, and poorly written API does not take care, so we prevent security issue here.
            user = null;
        }

        return user;
    }

    /**
     * Get the current entity from context.
     *
     * @return the current sdoc or doc document reference, or the current wiki reference if no doc available.
     */
    private EntityReference getCurrentEntity()
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument doc = xcontext.getDoc();

        if (doc != null) {
            return doc.getDocumentReference();
        }

        return null;
    }

    /**
     * Get the document used to test programming right.
     *
     * @return the current sdoc or doc document reference, null if no doc available.
     */
    private XWikiDocument getProgrammingDocument()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document = (XWikiDocument) xcontext.get(XWikiDocument.CKEY_SDOC);
        if (document == null) {
            document = xcontext.getDoc();
        }

        return document;
    }
}
