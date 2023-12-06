package org.xwiki.notifications.notifiers.internal;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.event.CleaningFilterEvent;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;

@Component(roles = DeletedDocumentCleanUpFilterProcessingQueue.class)
@Singleton
public class DeletedDocumentCleanUpFilterProcessingQueue implements Initializable, Disposable
{
    private class CleanUpFilterData
    {
        DocumentReference user;
        DocumentReference deletedDocument;

        CleanUpFilterData(DocumentReference user, DocumentReference deletedDocument)
        {
            this.user = user;
            this.deletedDocument = deletedDocument;
        }
    }

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
        this.cleanupQueue.clear();
        // check if we can add something in the queue to avoid error during cleanup method
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Start the clean up thread
        Thread optimizeThreadthread = new Thread(new ExecutionContextRunnable(this::cleanup, this.componentManager));
        optimizeThreadthread.setName("Pre filtering Live mail notification optimizer");
        optimizeThreadthread.setPriority(Thread.NORM_PRIORITY - 1);
        optimizeThreadthread.setDaemon(true);
        optimizeThreadthread.start();
    }

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

            this.performCleanup(cleanUpFilterData);
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
                    for (NotificationFilterPreference matchingPreference : matchingPreferences) {
                        this.notificationFilterPreferenceManager.deleteFilterPreference(user,
                            matchingPreference.getId());
                    }
                }
            }
        } catch (NotificationException e) {
            throw new RuntimeException(e);
        }
    }
}
