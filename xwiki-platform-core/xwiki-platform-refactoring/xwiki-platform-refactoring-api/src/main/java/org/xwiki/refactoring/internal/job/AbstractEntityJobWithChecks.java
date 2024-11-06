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
package org.xwiki.refactoring.internal.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.question.EntitySelection;

/**
 * Abstract job that create the list of pages to delete in order to do some checks and ask a confirmation to the user.
 *
 * @param <R> the request type
 * @param <S> the job status type
 * @version $Id$
 * @since 9.1RC1
 */
public abstract class AbstractEntityJobWithChecks<R extends EntityRequest, S extends EntityJobStatus<? super R>>
    extends AbstractEntityJob<R, S>
{
    /**
     * Map that will contain all entities that are concerned by the refactoring.
     * Note that the EntityReference key locale is automatically set to null if it's the Locale.ROOT.
     */
    protected final Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();

    @Override
    protected void runInternal() throws Exception
    {
        progressManager.pushLevelProgress(2, this);
        try {
            Collection<EntityReference> entityReferences = this.request.getEntityReferences();
            if (entityReferences != null) {
                // Get the list of concerned entities
                progressManager.startStep(this);
                getEntities(entityReferences);

                // Process
                progressManager.startStep(this);
                setContextUser();
                process(entityReferences);
            }
        } finally {
            progressManager.popLevelProgress(this);
        }
    }

    protected void getEntities(Collection<EntityReference> entityReferences)
    {
        this.progressManager.pushLevelProgress(entityReferences.size(), this);

        try {
            for (EntityReference entityReference : entityReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    getEntities(entityReference);
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    protected void getEntities(EntityReference entityReference)
    {
        // Dispatch the check operation based on the entity type.
        switch (entityReference.getType()) {
            case DOCUMENT:
                getEntities(new DocumentReference(entityReference));
                break;
            case SPACE:
                getEntities(new SpaceReference(entityReference));
                break;
            default:
                this.logger.error("Unsupported entity type [{}].", entityReference.getType());
        }
    }

    protected DocumentReference cleanLocale(DocumentReference documentReference)
    {
        // We don't want to have locale information for root locale in the reference to not have problems with
        // the questions.
        // FIXME: Note that this should be probably improved in the future, to actually put the locale in the question
        // along with the references, however it's difficult to support for now since we don't have a proper way to
        // convert a value to a DocumentReference with locale through a Converter, which is currently the standard way
        // for answering questions in QuestionJobResourceReferenceHandler.
        // Moreover it's difficult to use a String value that we'd parse afterwards since the question is not a
        // component.
        if (Locale.ROOT.equals(documentReference.getLocale())) {
            return new DocumentReference(documentReference, (Locale) null);
        } else {
            return documentReference;
        }
    }

    protected void putInConcernedEntities(DocumentReference documentReference)
    {
        DocumentReference cleanDocumentReference = cleanLocale(documentReference);
        this.concernedEntities.put(cleanDocumentReference, new EntitySelection(cleanDocumentReference));
    }

    protected void getEntities(DocumentReference documentReference)
    {
        if (this.request.isDeep() && isSpaceHomeReference(documentReference)) {
            getEntities(documentReference.getLastSpaceReference());
        } else {
            this.putInConcernedEntities(documentReference);
        }
    }

    protected void getEntities(SpaceReference spaceReference)
    {
        visitDocuments(spaceReference, this::putInConcernedEntities);
    }

    protected void notifyDocumentsDeleting()
    {
        // Allow others to exclude documents from being deleted.
        DocumentsDeletingEvent event = new DocumentsDeletingEvent();
        this.observationManager.notify(event, this, this.concernedEntities);

        // Stop the job if some listener has canceled the action.
        if (event.isCanceled()) {
            getStatus().cancel();
        }
    }

    protected EntitySelection getConcernedEntitiesEntitySelection(EntityReference reference)
    {
        EntitySelection entitySelection = this.concernedEntities.get(reference);
        if (entitySelection == null && reference instanceof DocumentReference) {
            DocumentReference documentReference = (DocumentReference) reference;
            if (Locale.ROOT.equals(documentReference.getLocale())) {
                entitySelection = this.concernedEntities.get(
                    new DocumentReference(documentReference.withoutLocale(), (Locale) null));
            }
        }
        return entitySelection;
    }
}
