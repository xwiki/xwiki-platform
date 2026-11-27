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
package org.xwiki.pagetemplate.internal;

import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * Updates the visibility and creation restrictions of the template providers when the space they target is renamed
 * or moved.
 * <p>
 * The restrictions are stored as space references in database list properties whose values come from a custom
 * query, which the link refactoring doesn't handle, so they would otherwise keep pointing to the old location. Like
 * the back-links, they are only updated when the rename or move request asks to update the links.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
@Component
@Named(TemplateProviderRestrictionsRenameListener.NAME)
@Singleton
public class TemplateProviderRestrictionsRenameListener extends AbstractLocalEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.pagetemplate.internal.TemplateProviderRestrictionsRenameListener";

    @Inject
    private Logger logger;

    @Inject
    private TemplateProviderRestrictionsUpdater restrictionsUpdater;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Default constructor.
     */
    public TemplateProviderRestrictionsRenameListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        DocumentRenamedEvent renamedEvent = (DocumentRenamedEvent) event;
        DocumentReference oldReference = renamedEvent.getSourceReference();
        DocumentReference newReference = renamedEvent.getTargetReference();

        // A restriction targets a space, which only follows the rename when the space home page stays a space home.
        if (!isSpaceHome(oldReference) || !isSpaceHome(newReference)) {
            return;
        }

        Predicate<DocumentReference> canEdit;
        if (data instanceof AbstractCopyOrMoveRequest request) {
            if (!request.isUpdateLinks()) {
                return;
            }
            canEdit = reference -> hasEditAccess(request, reference);
        } else {
            canEdit = reference -> this.contextualAuthorizationManager.hasAccess(Right.EDIT, reference);
        }

        SpaceReference oldSpace = oldReference.getLastSpaceReference();
        SpaceReference newSpace = newReference.getLastSpaceReference();
        try {
            this.restrictionsUpdater.replaceSpace(oldSpace, newSpace, canEdit);
        } catch (QueryException | XWikiException e) {
            this.logger.error("Failed to update the restrictions of the template providers targeting [{}] after "
                + "its rename to [{}].", oldSpace, newSpace, e);
        }
    }

    private boolean isSpaceHome(DocumentReference reference)
    {
        return this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName()
            .equals(reference.getName());
    }

    /**
     * Same check as the refactoring jobs, which test the user and the author of the request when asked to.
     */
    private boolean hasEditAccess(AbstractCopyOrMoveRequest request, DocumentReference reference)
    {
        return (!request.isCheckRights()
            || this.authorizationManager.hasAccess(Right.EDIT, request.getUserReference(), reference))
            && (!request.isCheckAuthorRights()
                || this.authorizationManager.hasAccess(Right.EDIT, request.getAuthorReference(), reference));
    }
}
