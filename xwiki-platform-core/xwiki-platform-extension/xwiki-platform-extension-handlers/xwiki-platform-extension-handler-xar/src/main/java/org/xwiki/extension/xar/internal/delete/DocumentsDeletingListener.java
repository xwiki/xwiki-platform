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
package org.xwiki.extension.xar.internal.delete;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.XarExtensionConfiguration;
import org.xwiki.extension.xar.XarExtensionConfiguration.DocumentProtection;
import org.xwiki.extension.xar.internal.delete.question.ExtensionBreakingQuestion;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;

/**
 * Listener that check if pages which are going to be deleted belong to extensions, and maybe ask the user what to do.
 *
 * @version $Id$
 * @since 9.1RC1
 */
@Component
@Singleton
@Named("DocumentsDeletingListener")
public class DocumentsDeletingListener extends AbstractEventListener
{
    private static final List<Event> EVENTS = Arrays.asList(new DocumentsDeletingEvent());

    @Inject
    @Named("xar")
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private XarExtensionConfiguration configuration;

    @Inject
    private Logger logger;

    /**
     * Construct a DocumentsDeletingListener.
     */
    public DocumentsDeletingListener()
    {
        super("XAR Extension Documents Deleting Listener", EVENTS);
    }

    private boolean shouldListenerBeTriggered(Job job, CancelableEvent event)
    {
        if (event.isCanceled()) {
            logger.debug("Skipping [{}] as the event is already cancelled.", this.getName());
            return false;
        }

        if (this.configuration.getDocumentProtection() == DocumentProtection.NONE) {
            return false;
        }

        if (!job.getRequest().isInteractive()) {
            logger
                .warn("XAR Extension Documents Deleting Listener will not check the document in non-interactive mode.");
            return false;
        }

        return true;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Job job = (Job) source;
        CancelableEvent cancelableEvent = (CancelableEvent) event;

        if (!this.shouldListenerBeTriggered(job, cancelableEvent)) {
            return;
        }

        // Check if some pages belong to extensions
        Map<EntityReference, EntitySelection> concernedEntities = (Map<EntityReference, EntitySelection>) data;
        ExtensionBreakingQuestion question = new ExtensionBreakingQuestion(concernedEntities);
        for (EntitySelection entitySelection : concernedEntities.values()) {
            if (entitySelection.getEntityReference() instanceof DocumentReference) {
                checkIfDeleteIsAllowed(entitySelection, question);
            }
        }

        JobStatus jobStatus = job.getStatus();
        // Ask a confirmation to the user if some pages belong to extensions
        if (!question.getExtensions().isEmpty()) {
            // Conservative choice: we let the user enable the pages to delete.
            question.unselectAll();
            try {
                // The user can modify the question so it could disable some EntitySelection.
                // We add a timeout because when a refactoring job is running, it prevents others to run.
                // 5 minutes is probably enough for the user to decide if the process should go on.
                boolean ack = jobStatus.ask(question, 5, TimeUnit.MINUTES);
                if (!ack) {
                    // Without any confirmation, we must cancel the operation.
                    String message = "The question has been asked, however no answer has been received.";
                    this.logger.warn(message);
                    cancelableEvent.cancel(message);
                }
            } catch (InterruptedException e) {
                this.logger.warn("Confirm question has been interrupted.");
                cancelableEvent.cancel("Question has been interrupted.");
            }
            // we always want the event and the CancelableJobStatus to be consistent
            if (jobStatus instanceof CancelableJobStatus) {
                CancelableJobStatus cancelableJobStatus = (CancelableJobStatus) jobStatus;
                if (cancelableJobStatus.isCanceled()) {
                    cancelableEvent.cancel();
                }
                if (cancelableEvent.isCanceled()) {
                    cancelableJobStatus.cancel();
                }
            }
        }
    }

    private void checkIfDeleteIsAllowed(EntitySelection entitySelection, ExtensionBreakingQuestion question)
    {
        XarInstalledExtensionRepository repository = (XarInstalledExtensionRepository) installedExtensionRepository;
        DocumentReference documentReference = (DocumentReference) entitySelection.getEntityReference();

        if (repository.isAllowed(documentReference, Right.DELETE)) {
            question.markAsFreePage(entitySelection);

            return;
        }

        Collection<XarInstalledExtension> extensions = repository.getXarInstalledExtensions(documentReference);

        for (XarInstalledExtension extension : extensions) {
            question.pageBelongsToExtension(entitySelection, extension);
        }
    }
}
