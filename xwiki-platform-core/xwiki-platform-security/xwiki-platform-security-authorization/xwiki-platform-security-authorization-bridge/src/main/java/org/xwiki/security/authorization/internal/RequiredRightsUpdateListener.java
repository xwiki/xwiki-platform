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

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.document.RequiredRights;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserCreatingDocumentEvent;
import com.xpn.xwiki.internal.event.UserDeletingDocumentEvent;
import com.xpn.xwiki.internal.event.UserEvent;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;

/**
 * Cancel any modification on required rights that the user does not have the right to do.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(RequiredRightsUpdateListener.NAME)
public class RequiredRightsUpdateListener extends AbstractEventListener
{
    /**
     * The unique name of the listener.
     */
    public static final String NAME = "org.xwiki.security.authorization.internal.RequiredRightsUpdateListener";

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Execution execution;

    /**
     * Default constructor.
     */
    public RequiredRightsUpdateListener()
    {
        super(NAME, new UserUpdatingDocumentEvent(), new UserCreatingDocumentEvent(), new UserDeletingDocumentEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        UserEvent userEvent = (UserEvent) event;

        XWikiDocument originalDocument = document.getOriginalDocument();

        RequiredRights originalRights = originalDocument.getRequiredRights();
        RequiredRights newRights = document.getRequiredRights();

        if (!Objects.equals(originalRights, newRights)) {
            if (newRights.activated() && missesRequiredRight(userEvent.getUserReference(), document)) {
                ((CancelableEvent) event).cancel("Setting more rights than the user has is forbidden.");
            }

            if (originalRights.activated() && missesRequiredRight(userEvent.getUserReference(), originalDocument)) {
                ((CancelableEvent) event).cancel("Updating a document whose rights the user doesn't have is forbidden");
            }
        }
    }

    private boolean missesRequiredRight(DocumentReference userReference, XWikiDocument document)
    {
        ExecutionContext context = this.execution.getContext();
        Object previousValue = context.getProperty(RequiredRightsSkipContext.SKIP_REQUIRED_RIGHT);
        context.setProperty(RequiredRightsSkipContext.SKIP_REQUIRED_RIGHT, String.valueOf(Boolean.TRUE));
        try {
            // Check that the user has all required rights.
            return !document.getRequiredRights().getRights().stream()
                .allMatch(r -> this.authorizationManager.hasAccess(r, userReference, document.getDocumentReference()));
        } finally {
            context.setProperty(RequiredRightsSkipContext.SKIP_REQUIRED_RIGHT, previousValue);
        }
    }
}
