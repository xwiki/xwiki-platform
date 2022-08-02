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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.security.authorization.Right;

/**
 * A job that can replace the occurrences or an user reference with another user reference.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
public abstract class AbstractReplaceUserJob
    extends AbstractEntityJob<ReplaceUserRequest, EntityJobStatus<ReplaceUserRequest>>
{
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String getType()
    {
        return RefactoringJobs.REPLACE_USER;
    }

    @Override
    protected void runInternal() throws Exception
    {
        DocumentReference oldUserReference = getRequest().getOldUserReference();
        DocumentReference newUserReference = getRequest().getNewUserReference();
        if (!Objects.equals(oldUserReference, newUserReference) && isSomethingToReplace()) {
            if (oldUserReference != null && newUserReference != null) {
                super.runInternal();
            } else {
                this.logger.error("Replacing the guest user is not supported.");
            }
        }
    }

    private boolean isSomethingToReplace()
    {
        return getRequest().isReplaceDocumentAuthor() || getRequest().isReplaceDocumentContentAuthor()
            || getRequest().isReplaceDocumentCreator();
    }

    @Override
    protected void process(EntityReference entityReference)
    {
        if (hasAccess(Right.ADMIN, entityReference)) {
            update(getDocumentsToUpdate(entityReference));
        } else {
            this.logger.error("You need administration right on [{}] in order to be able to replace the user.",
                entityReference);
        }
    }

    private List<DocumentReference> getDocumentsToUpdate(EntityReference entityReference)
    {
        if (entityReference.getType() != EntityType.WIKI && entityReference.getType() != EntityType.SPACE) {
            this.logger.warn("Skipping unsupported entity [{}].", entityReference);
            return Collections.emptyList();
        }

        try {
            this.logger.info("Updating documents from [{}].", entityReference);
            return getDocumentsToUpdateQuery(entityReference).<Object[]>execute().stream()
                .map(this.resolveDocumentReferenceWithLocale(entityReference)).collect(Collectors.toList());
        } catch (QueryException e) {
            this.logger.error("Failed to retrieve the list of documents to update from [{}]. Root cause is [{}].",
                entityReference, ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }

    private Query getDocumentsToUpdateQuery(EntityReference parentReference) throws QueryException
    {
        List<String> oldUserConstraints = new ArrayList<>();
        if (getRequest().isReplaceDocumentAuthor()) {
            oldUserConstraints.add("doc.author = :oldUser");
        }
        if (getRequest().isReplaceDocumentContentAuthor()) {
            oldUserConstraints.add("doc.contentAuthor = :oldUser");
        }
        if (getRequest().isReplaceDocumentCreator()) {
            oldUserConstraints.add("doc.creator = :oldUser");
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put("oldUser",
            this.compactWikiEntityReferenceSerializer.serialize(getRequest().getOldUserReference(), parentReference));

        List<String> constraints = new ArrayList<>();
        constraints.add("(" + StringUtils.join(oldUserConstraints, " OR ") + ")");

        EntityReference spaceReference = parentReference.extractReference(EntityType.SPACE);
        if (spaceReference != null) {
            constraints.add("doc.space = :space");
            parameters.put("space",
                this.compactWikiEntityReferenceSerializer.serialize(spaceReference, parentReference));
        }

        String statement = "select doc.fullName, doc.language from XWikiDocument as doc where "
            + StringUtils.join(constraints, " AND ");
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(parentReference.extractReference(EntityType.WIKI).getName());

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }

        return query;
    }

    private Function<Object[], DocumentReference> resolveDocumentReferenceWithLocale(EntityReference parentReference)
    {
        return (result -> new DocumentReference(documentReferenceResolver.resolve((String) result[0], parentReference),
            LocaleUtils.toLocale((String) result[1])));
    }

    private void update(List<DocumentReference> documentReferences)
    {
        this.progressManager.pushLevelProgress(documentReferences.size(), this);

        try {
            for (DocumentReference documentReference : documentReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    this.logger.info("Updating document [{}].", documentReference);
                    update(documentReference);
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * Updates the occurrences of the old user reference on the specified document.
     * 
     * @param documentReference the document to update
     */
    protected abstract void update(DocumentReference documentReference);
}
