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
package org.xwiki.notifications.filters.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.user.internal.group.UsersCache;

/**
 * Listener aiming at cleaning up filter preferences whenever a document is deleted as part of a folded event.
 * Cleaning up of filter preferences occurring during unfolded events are directly managed in the
 * {@code UserEventDispatcher} so that notifications are processed before cleaning up the filter.
 *
 * @version $Id$
 * @since 15.10.2
 * @since 16.0.0RC1
 */
@Component
@Named(DeletedDocumentCleanUpFilterListener.NAME)
@Singleton
public class DeletedDocumentCleanUpFilterListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.notifications.notifiers.internal.DeletedDocumentCleanUpFilterListener";

    private static final BeginFoldEvent FOLDED_EVENTS = otherEvent -> otherEvent instanceof BeginFoldEvent;
    private static final DocumentRenamingEvent DOCUMENT_RENAMING_EVENT = new DocumentRenamingEvent();

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Provider<DeletedDocumentCleanUpFilterProcessingQueue> cleanUpFilterProcessingQueueProvider;

    @Inject
    private Provider<UsersCache> usersCacheProvider;

    /**
     * Default constructor.
     */
    public DeletedDocumentCleanUpFilterListener()
    {
        super(NAME, new DocumentDeletedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        // Document rename usecase is handled with its own listener.
        if (this.observationContext.isIn(FOLDED_EVENTS) && !this.observationContext.isIn(DOCUMENT_RENAMING_EVENT))
        {
            DocumentDeletedEvent documentDeletedEvent = (DocumentDeletedEvent) event;
            DocumentReference documentReference = documentDeletedEvent.getDocumentReference();
            List<DocumentReference> users =
                this.usersCacheProvider.get().getUsers(documentReference.getWikiReference(), false);
            for (DocumentReference user : users) {
                this.cleanUpFilterProcessingQueueProvider.get().addCleanUpTask(user, documentReference);
            }
        }
    }
}
