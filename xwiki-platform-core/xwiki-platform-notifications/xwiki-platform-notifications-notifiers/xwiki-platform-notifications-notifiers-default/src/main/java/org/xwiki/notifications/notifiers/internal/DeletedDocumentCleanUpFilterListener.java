package org.xwiki.notifications.notifiers.internal;

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
import org.xwiki.user.internal.group.UsersCache;

@Component
@Named(DeletedDocumentCleanUpFilterListener.NAME)
@Singleton
public class DeletedDocumentCleanUpFilterListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.notifications.notifiers.internal.DeletedDocumentCleanUpFilterListener";

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Provider<DeletedDocumentCleanUpFilterProcessingQueue> cleanUpFilterProcessingQueueProvider;

    @Inject
    private Provider<UsersCache> usersCacheProvider;

    private static final BeginFoldEvent IGNORED_EVENTS = otherEvent -> otherEvent instanceof BeginFoldEvent;

    public DeletedDocumentCleanUpFilterListener()
    {
        super(NAME, new DocumentDeletedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (this.observationContext.isIn(IGNORED_EVENTS)) {
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
