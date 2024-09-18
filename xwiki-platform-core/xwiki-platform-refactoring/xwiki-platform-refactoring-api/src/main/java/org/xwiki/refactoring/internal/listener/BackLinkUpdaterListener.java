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
package org.xwiki.refactoring.internal.listener;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.RefactoringException;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.refactoring.internal.job.DeleteJob;
import org.xwiki.refactoring.internal.job.MoveJob;
import org.xwiki.refactoring.job.AbstractCopyOrMoveRequest;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Updates the back-links after a document has been renamed or deleted.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Named(BackLinkUpdaterListener.NAME)
@Singleton
public class BackLinkUpdaterListener extends AbstractLocalEventListener
{
    /**
     * The name of this event listener.
     */
    public static final String NAME = "refactoring.backLinksUpdater";

    @Inject
    private Logger logger;

    @Inject
    private ReferenceUpdater updater;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private JobContext jobContext;

    // Use a Provider to avoid early initialization of dependencies.
    @Inject
    private Provider<LinkIndexingWaitingHelper> linkIndexingHelper;

    /**
     * Default constructor.
     */
    public BackLinkUpdaterListener()
    {
        super(NAME, new DocumentRenamedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        try {
            if (event instanceof DocumentRenamedEvent) {
                maybeUpdateLinksAfterRename(event, source, data);
            } else if (event instanceof DocumentDeletedEvent && this.jobContext.getCurrentJob() instanceof DeleteJob) {
                maybeUpdateLinksAfterDelete(event);
            }
        } catch (RefactoringException e) {
            this.logger.error("Failed to update links backlinks", e);
        }
    }

    private void maybeUpdateLinksAfterDelete(Event event) throws RefactoringException
    {
        DeleteJob job = (DeleteJob) this.jobContext.getCurrentJob();
        DeleteRequest request = (DeleteRequest) job.getRequest();
        Predicate<EntityReference> canEdit = entityReference -> (job.hasAccess(Right.EDIT, entityReference));
        DocumentDeletedEvent deletedEvent = (DocumentDeletedEvent) event;

        DocumentReference newTarget = request.getNewBacklinkTargets().get(deletedEvent.getDocumentReference());
        if (request.isUpdateLinks() && newTarget != null) {
            updateBackLinks(deletedEvent.getDocumentReference(), newTarget, canEdit);
        }
    }

    private void maybeUpdateLinksAfterRename(Event event, Object source, Object data) throws RefactoringException
    {
        boolean updateLinks = true;
        Predicate<EntityReference> canEdit =
            entityReference -> this.authorization.hasAccess(Right.EDIT, entityReference);

        if (source instanceof MoveJob) {
            MoveRequest request = (MoveRequest) data;
            updateLinks = request.isUpdateLinks();
            // Check access rights taking into account the move request.
            canEdit = entityReference -> ((MoveJob) source).hasAccess(Right.EDIT, entityReference);
        }

        if (updateLinks) {
            DocumentRenamedEvent renameEvent = (DocumentRenamedEvent) event;
            updateBackLinks(renameEvent.getSourceReference(), renameEvent.getTargetReference(), canEdit);
        }
    }

    private void updateBackLinks(DocumentReference source, DocumentReference target, Predicate<EntityReference> canEdit)
        throws RefactoringException
    {
        this.logger.info("Updating the back-links for document [{}].", source);

        if (isInsideJobAndShallWaitForIndexing()) {
            // We're inside a job, so some waiting should be okay. Wait for the indexing of the link store to finish.
            try {
                this.logger.info("Waiting for the link index to be updated.");
                this.linkIndexingHelper.get().waitWithQuestion(10, TimeUnit.SECONDS);
                this.logger.info("Finished waiting for the link index, starting the update of backlinks.");
            } catch (InterruptedException e) {
                this.logger.warn(
                    "Interrupted while waiting for link indexing: [{}], continuing with the update of the"
                        + " backlinks nevertheless.", ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full interrupted exception:", e);
                Thread.currentThread().interrupt();
            } catch (RefactoringException e) {
                this.logger.warn(
                    "Failed to wait for the link index to be updated: [{}], continuing with the update of"
                        + " the backlinks nevertheless.", ExceptionUtils.getRootCauseMessage(e));
                this.logger.debug("Full exception:", e);
            }
        }

        // TODO: it's possible to optimize a bit the actual entities to modify (especially which translation of the
        // document to load and parse) since we have the information in the store
        Set<DocumentReference> backlinkDocumentReferences = this.modelBridge.getBackLinkedDocuments(source);

        this.progressManager.pushLevelProgress(backlinkDocumentReferences.size(), this);

        try {
            for (DocumentReference backlinkDocumentReference : backlinkDocumentReferences) {
                this.progressManager.startStep(this);
                if (canEdit.test(backlinkDocumentReference)) {
                    this.updater.update(backlinkDocumentReference, source, target);
                }
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private boolean isInsideJobAndShallWaitForIndexing()
    {
        Job currentJob = this.jobContext.getCurrentJob();

        boolean result;
        if (currentJob != null) {
            Request request = currentJob.getRequest();
            if (request instanceof DeleteRequest deleteRequest) {
                result = deleteRequest.isWaitForIndexing();
            } else if (request instanceof AbstractCopyOrMoveRequest copyOrMoveRequest) {
                result = copyOrMoveRequest.isWaitForIndexing();
            } else {
                result = true;
            }
        } else {
            result = false;
        }

        return result;
    }
}
