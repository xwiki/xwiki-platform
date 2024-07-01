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

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.event.CleaningFilterEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Component in charge of processing tasks for cleaning up filters whenever a document is deleted.
 * This component creates its own low priority thread for this task.
 * Implementation inspired from {@code PrefilteringLiveNotificationEmailManager}.
 *
 * @version $Id$
 * @since 15.10.2
 * @since 16.0.0RC1
 */
@Component(roles = DeletedDocumentCleanUpFilterProcessingQueue.class)
@Singleton
public class DeletedDocumentCleanUpFilterProcessingQueue implements Initializable, Disposable
{
    private static class CleanUpFilterData
    {
        private final DocumentReference user;
        private final DocumentReference deletedDocument;

        CleanUpFilterData(DocumentReference user, DocumentReference deletedDocument)
        {
            this.user = user;
            this.deletedDocument = deletedDocument;
        }
    }

    private static final CleanUpFilterData STOP_DATA = new CleanUpFilterData(null, null);

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private Logger logger;

    private final BlockingQueue<CleanUpFilterData> cleanupQueue = new LinkedBlockingQueue<>();

    private boolean disposed;

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;

        // Note that this implies that some filters might never be clean up:
        // FIXME: this needs to be properly fixed in the future by refactoring all that once XWIKI-21692 is done
        this.cleanupQueue.clear();
        this.cleanupQueue.add(STOP_DATA);
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Start the clean up thread
        Thread optimizeThreadthread = new Thread(new ExecutionContextRunnable(this::cleanup, this.componentManager));
        optimizeThreadthread.setName("Notification filters clean up queue");
        optimizeThreadthread.setPriority(Thread.NORM_PRIORITY - 1);
        optimizeThreadthread.setDaemon(true);
        optimizeThreadthread.start();
    }

    /**
     * Add a new task for cleaning up all {@link NotificationFilterPreference} owned by the given user and concerning
     * the given document references.
     * @param user the user for whom to delete filters for
     * @param deletedDocument the document that has been deleted and for which to clean up the filters
     */
    public void addCleanUpTask(DocumentReference user, DocumentReference deletedDocument)
    {
        if (!this.disposed) {
            // check if the data has been added and log
            this.cleanupQueue.add(new CleanUpFilterData(user, deletedDocument));
        }
    }

    private void cleanup()
    {
        while (!this.disposed) {
            CleanUpFilterData cleanUpFilterData;
            try {
                cleanUpFilterData = this.cleanupQueue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The thread handling filter clean up has been interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }

            if (cleanUpFilterData != STOP_DATA) {
                this.performCleanup(cleanUpFilterData);
            }
        }
    }

    private void performCleanup(CleanUpFilterData data)
    {
        DocumentReference deletedDocumentReference = data.deletedDocument;
        DocumentReference user = data.user;
        String serializedReference = this.entityReferenceSerializer.serialize(deletedDocumentReference);
        try {
            Set<NotificationFilterPreference> matchingPreferences =
                this.notificationFilterPreferenceManager.getFilterPreferences(user)
                    // We only clean up the filters matching exactly the page reference
                    // we never clean up filters for space / wiki there
                    .stream().filter(pref -> StringUtils.equals(pref.getPageOnly(), serializedReference))
                    .collect(Collectors.toSet());
            if (!matchingPreferences.isEmpty()) {
                CleaningFilterEvent cleaningFilterEvent = new CleaningFilterEvent();
                this.observationManager.notify(cleaningFilterEvent, deletedDocumentReference, matchingPreferences);
                if (!cleaningFilterEvent.isCanceled()) {
                    this.notificationFilterPreferenceManager.deleteFilterPreferences(user,
                        matchingPreferences.stream()
                            .map(NotificationFilterPreference::getId)
                            .collect(Collectors.toSet()));
                }
            }
        } catch (NotificationException e) {
            this.logger.error("Error while trying to clean up filter for user [{}] and document [{}]", user,
                deletedDocumentReference, e);
        }
    }
}
