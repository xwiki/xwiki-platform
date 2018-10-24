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
package org.xwiki.refactoring.job;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.question.EntitySelection;

/**
 * Listener that check if pages which are going to be deleted contain an XClass which is used somewhere in the wiki.
 *
 * @version $Id$
 * @since 10.9
 */
@Component
@Singleton
@Named("XClassDeletingListener")
public class XClassDeletingListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private EntityReferenceResolver<String> resolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Construct an XClassDeletingListener.
     */
    public XClassDeletingListener()
    {
        super("XClass Deleting Listener", new DocumentsDeletingEvent());
    }

    private XClassBreakingQuestion buildQuestion(Job job, CancelableEvent event, Object data)
    {
        JobStatus jobStatus = job.getStatus();
        if (event.isCanceled()
            || jobStatus instanceof CancelableJobStatus && ((CancelableJobStatus) jobStatus).isCanceled()) {
            logger.debug("Skipping [{}] as the event is already cancelled.", this.getName());
            return null;
        }

        if (!job.getRequest().isInteractive()) {
            logger
                .warn("XClass deleting listener will not check the document in non-interactive mode.");
            return null;
        }

        // Check if some pages contain used XClass
        Map<EntityReference, EntitySelection> concernedEntities = (Map<EntityReference, EntitySelection>) data;
        XClassBreakingQuestion question = new XClassBreakingQuestion(concernedEntities);
        for (EntitySelection entitySelection : concernedEntities.values()) {
            if (entitySelection.getEntityReference() instanceof DocumentReference) {
                checkIfDeleteIsAllowed(entitySelection, question);
            }
        }

        return question;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Job job = (Job) source;
        CancelableEvent cancelableEvent = (CancelableEvent) event;
        XClassBreakingQuestion question = this.buildQuestion(job, cancelableEvent, data);

        if (question == null) {
            return;
        }
        // Ask a confirmation to the user if some pages contain used XClass
        if (!question.getImpactedObjects().isEmpty()) {

            try {
                if (this.documentAccessBridge.isAdvancedUser(job.getRequest().getProperty("user.reference"))) {
                    // Conservative choice: we let the user enable the pages to delete.
                    question.unselectAll();
                    // The user can modify the question so it could disable some EntitySelection.
                    // We add a timeout because when a refactoring job is running, it prevents others to run.
                    // 5 minutes is probably enough for the user to decide if the process should go on.
                    boolean ack = job.getStatus().ask(question, 5, TimeUnit.MINUTES);
                    if (!ack) {
                        // Without any confirmation, we must cancel the operation.
                        String message = "The question has been asked, however no answer has been received.";
                        this.logger.warn(message);
                        cancelableEvent.cancel(message);
                    }
                } else {
                    question.setRefactoringForbidden(true);
                    // we don't want the user to answer the question,
                    // but we want to display that his action is forbidden.
                    boolean ack = job.getStatus().ask(question, 1, TimeUnit.MINUTES);
                    if (!ack) {
                        String message = "The question has been canceled because this refactoring is forbidden.";
                        cancelableEvent.cancel(message);
                    }
                }
            } catch (InterruptedException e) {
                this.logger.warn("Confirm question has been interrupted.");
                cancelableEvent.cancel("Question has been interrupted.");
            }
        }
    }

    private void checkIfDeleteIsAllowed(EntitySelection entitySelection, XClassBreakingQuestion question)
    {
        int queryLimit = 25;
        DocumentReference classReference = (DocumentReference) entitySelection.getEntityReference();

        String query = "select distinct obj.name from BaseObject obj where obj.className=:className "
            + "order by obj.name asc";

        String className = localSerializer.serialize(classReference);
        try {

            List<String> results = this.queryManager.createQuery(query, Query.HQL)
                .setLimit(queryLimit)
                .bindValue("className", className)
                .setWiki(classReference.getWikiReference().getName())
                .<String>execute();

            if (results.isEmpty()) {
                question.markAsFreePage(entitySelection);
            } else {
                if (results.size() == queryLimit) {
                    question.setObjectsPotentiallyHidden(true);
                }
                for (String documentObjectName : results) {
                    EntityReference documentObjectReference = this.resolver.resolve(documentObjectName,
                        EntityType.DOCUMENT, classReference);
                    question.markImpactedObject(entitySelection, documentObjectReference);
                }
            }
        } catch (QueryException e) {
            logger.error("Error while executing query to retrieve objects linked to an XClass.", e);
        }
    }
}
