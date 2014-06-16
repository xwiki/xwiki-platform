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
import org.xwiki.model.reference.WikiReference;
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
        checkAccess(right, getCurrentEntity(right));
    }

    @Override
    public void checkAccess(Right right, EntityReference entity) throws AccessDeniedException
    {
        DocumentReference user = getCurrentUser(right, entity);

        if (!checkPreAccess(right)) {
            throw new AccessDeniedException(right, user, entity);
        }

        authorizationManager.checkAccess(right, user, entity);
    }

    @Override
    public boolean hasAccess(Right right)
    {
        return hasAccess(right, getCurrentEntity(right));
    }

    @Override
    public boolean hasAccess(Right right, EntityReference entity)
    {
        DocumentReference user = getCurrentUser(right, entity);

        return checkPreAccess(right) && authorizationManager.hasAccess(right, user, entity);
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
            if (renderingContext.isRestricted() || xcontextProvider.get().hasDroppedPermissions()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return the current user from context.
     */
    private DocumentReference getCurrentUser(Right right, EntityReference entity)
    {
        // Backward compatibility for the old way of assigning programming right.
        if (right == Right.PROGRAM) {
            XWikiDocument doc = getDocument(entity);
            if (doc != null) {
                return getContentAuthor(doc);
            }
        }
        return xcontextProvider.get().getUserReference();
    }

    /**
     * Retrieve the document corresponding to the given reference.
     *
     * @param entity an entity reference. The document reference is extracted from it.
     * @return a document or null if the reference could not be resolved to a document.
     */
    private XWikiDocument getDocument(EntityReference entity)
    {
        if (entity == null) {
            return null;
        }

        EntityReference docEntity = entity.extractReference(EntityType.DOCUMENT);
        if (docEntity == null) {
            return null;
        }

        XWikiContext context = xcontextProvider.get();
        try {
            return context.getWiki().getDocument(new DocumentReference(docEntity), context);
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

        return null;
    }

    /**
     * Get the current entity from context.
     *
     * @param right the right being checked.
     * @return the current sdoc or doc document reference, or the current wiki reference if no doc available.
     */
    private EntityReference getCurrentEntity(Right right)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWikiDocument doc = null;

        if (right == Right.PROGRAM) {
            doc = (XWikiDocument) xcontext.get("sdoc");
        }
        if (doc == null) {
            doc = xcontext.getDoc();
        }
        if (doc != null) {
            if (right == Right.PROGRAM) {
                return doc.getDocumentReference().getWikiReference();
            }
            return doc.getDocumentReference();
        }

        return new WikiReference(xcontext.getWikiId());
    }
}
