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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A job that can move entities to a new parent within the hierarchy.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named(MoveJob.JOB_TYPE)
public class MoveJob extends AbstractEntityJob<MoveRequest, EntityJobStatus<MoveRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "moveEntities";

    /**
     * Regular expression used to match the special characters supported by the like HQL operator (plus the escaping
     * character).
     * 
     * @see #getChildren(DocumentReference)
     */
    private static final Pattern LIKE_SPECIAL_CHARS = Pattern.compile("([%_/])");

    /**
     * Used to distinguish between terminal and non-terminal (WebHome) documents.
     * 
     * @see #isTerminal(EntityReference)
     */
    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Used to check access permissions.
     * 
     * @see #hasAccess(Right, EntityReference)
     */
    @Inject
    private AuthorizationManager authorization;

    /**
     * Used to perform the low level operations on entities.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to serialize a space reference in order to query the child document.
     * 
     * @see #getChildren(DocumentReference)
     */
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to resolve the references of child documents.
     * 
     * @see #getChildren(DocumentReference)
     */
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    /**
     * Used to query the child documents.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Specifies whether all entities with the same name are to be overwritten on not. When {@code true} all entities
     * with the same name are overwritten. When {@code false} all entities with the same name are skipped. If
     * {@code null} then a question is asked for each entity.
     */
    private Boolean overwriteAll;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected EntityJobStatus<MoveRequest> createNewStatus(MoveRequest request)
    {
        return new EntityJobStatus<MoveRequest>(request, this.observationManager, this.loggerManager, null);
    }

    @Override
    protected void runInternal() throws Exception
    {
        if (this.request.getDestination() != null) {
            super.runInternal();
        }
    }

    @Override
    protected void process(EntityReference source)
    {
        // Perform generic checks that don't depend on the source/destination type.

        EntityReference destination = this.request.getDestination();
        if (isDescendantOrSelf(destination, source)) {
            this.logger.error("Cannot make [{}] a descendant of itself.", source);
            return;
        }

        // Dispatch the move operation based on the source entity type.

        switch (source.getType()) {
            case DOCUMENT:
                move(new DocumentReference(source), destination);
                break;
            default:
                this.logger.warn("Unsupported source entity type [{}].", source.getType());
        }
    }

    private boolean isDescendantOrSelf(EntityReference alice, EntityReference bob)
    {
        EntityReference parent = alice;
        while (parent != null && !parent.equals(bob)) {
            parent = parent.getParent();
        }
        return parent != null;
    }

    private void move(DocumentReference source, EntityReference destination)
    {
        // Compute the reference of the destination document.

        EntityReference currentParent = isTerminal(source) ? source.getParent() : source.getParent().getParent();
        EntityReference newReference = source.removeParent(currentParent);

        EntityReference newParent = destination;
        if (destination.getType() == EntityType.DOCUMENT) {
            if (isTerminal(destination)) {
                this.logger.warn("The destination document [{}] cannot have child documents.", destination);
                return;
            } else {
                // The destination is a WebHome (nested) document so the new parent is its parent space.
                newParent = destination.getParent();
            }
        } else if (destination.getType() != EntityType.SPACE
            && (destination.getType() != EntityType.WIKI || isTerminal(source))) {
            this.logger.warn("Unsupported destination entity type [{}].", destination.getType());
            return;
        }

        newReference = newReference.appendParent(newParent);
        maybeMove(source, new DocumentReference(newReference));
    }

    private boolean isTerminal(EntityReference entityReference)
    {
        return entityReference.getName().equals(
            this.defaultEntityReferenceProvider.getDefaultReference(entityReference.getType()));
    }

    private void maybeMove(DocumentReference oldReference, DocumentReference newReference)
    {
        // Perform checks that are specific to the document source/destination type.

        if (!exists(oldReference)) {
            this.logger.warn("Skipping [{}] because it doesn't exist.", oldReference);
            return;
        }

        // The move operation is currently implemented as Copy + Delete.
        if (!hasAccess(Right.DELETE, oldReference)) {
            this.logger.warn("You are not allowed to move [{}].", oldReference);
            return;
        }

        if (!hasAccess(Right.VIEW, newReference) || !hasAccess(Right.EDIT, newReference)
            || (exists(newReference) && !hasAccess(Right.DELETE, newReference))) {
            this.logger.warn("You don't have sufficient permissions over the destination document [{}].", newReference);
            return;
        }

        move(oldReference, newReference, this.request.isDeep());
    }

    private void move(DocumentReference oldReference, DocumentReference newReference, boolean deep)
    {
        if (exists(newReference)) {
            if (this.request.isInteractive() && !confirmOverwrite(oldReference, newReference)) {
                this.logger.warn(
                    "Skipping [{}] because [{}] already exists and the user doesn't want to overwrite it.",
                    oldReference, newReference);
                return;
            } else if (!delete(newReference)) {
                return;
            }
        }

        if (!copy(oldReference, newReference)) {
            return;
        }

        // TODO: Update back links
        // TODO: Refactor links from doc content
        delete(oldReference);

        this.logger.info("Document [{}] has been moved to [{}].", oldReference, newReference);

        if (deep && !isTerminal(oldReference)) {
            moveChildren(oldReference, newReference);
        }
    }

    private boolean confirmOverwrite(EntityReference source, EntityReference destination)
    {
        if (this.overwriteAll == null) {
            OverwriteQuestion question = new OverwriteQuestion(source, destination);
            try {
                this.status.ask(question);
                if (!question.isAskAgain()) {
                    // Use the same answer for the following overwrite questions.
                    this.overwriteAll = question.isOverwrite();
                }
                return question.isOverwrite();
            } catch (InterruptedException e) {
                this.logger.warn("Overwrite question has been interrupted.");
                return false;
            }
        } else {
            return this.overwriteAll;
        }
    }

    private void moveChildren(DocumentReference oldReference, DocumentReference newReference)
    {
        List<DocumentReference> oldChildReferences = getChildren(oldReference);

        this.progressManager.pushLevelProgress(oldChildReferences.size(), this);

        try {
            for (DocumentReference oldChildReference : oldChildReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);

                    DocumentReference newChildReference =
                        oldChildReference.replaceParent(oldReference.getParent(), newReference.getParent());
                    // We don't have to move recursively because #getChildDocuments() returns all the descendants
                    // actually (because we don't have a way to retrieve the direct child documents at the moment).
                    move(oldChildReference, newChildReference, false);

                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private boolean copy(DocumentReference source, DocumentReference destination)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.request.getUserReference());
            return xcontext.getWiki().copyDocument(source, destination, false, xcontext);
        } catch (Exception e) {
            this.logger.warn("Failed to copy [{}] to [{}].", source, destination, e);
            return false;
        } finally {
            xcontext.setUserReference(userReference);
        }
    }

    private boolean delete(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(this.request.getUserReference());
            XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);
            xcontext.getWiki().deleteAllDocuments(document, xcontext);
            return true;
        } catch (Exception e) {
            this.logger.warn("Failed to delete document [{}].", reference, e);
            return false;
        } finally {
            xcontext.setUserReference(userReference);
        }
    }

    private boolean exists(DocumentReference reference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        return xcontext.getWiki().exists(reference, xcontext);
    }

    private boolean hasAccess(Right right, EntityReference reference)
    {
        return !this.request.isCheckRights()
            || this.authorization.hasAccess(right, this.request.getUserReference(), reference);
    }

    /**
     * @param documentReference the reference to a non-terminal (WebHome) document
     * @return the list of references to the child documents of the specified parent document
     */
    private List<DocumentReference> getChildren(DocumentReference documentReference)
    {
        try {
            // We don't have a way to retrieve only the direct children so we select all the descendants. This means we
            // select all the documents from the same space (excluding the given document) and from all the nested
            // spaces.
            String statement =
                "select distinct(doc.fullName) from XWikiDocument as doc "
                    + "where (doc.space = :space and doc.name <> :name) or doc.space like :spacePrefix escape '/'";
            Query query = this.queryManager.createQuery(statement, "hql");
            query.setWiki(documentReference.getWikiReference().getName());
            String localSpaceReference = this.localEntityReferenceSerializer.serialize(documentReference.getParent());
            query.bindValue("space", localSpaceReference).bindValue("name", documentReference.getName());
            String spacePrefix = LIKE_SPECIAL_CHARS.matcher(localSpaceReference).replaceAll("/$1");
            query.bindValue("spacePrefix", spacePrefix + ".%");

            List<DocumentReference> children = new ArrayList<>();
            for (Object fullName : query.execute()) {
                children.add(this.explicitDocumentReferenceResolver.resolve((String) fullName, documentReference));
            }
            return children;
        } catch (Exception e) {
            this.logger.warn("Failed to retrieve the child documents of [{}].", documentReference, e);
            return Collections.emptyList();
        }
    }

    @Override
    protected String getTargetWiki()
    {
        List<EntityReference> entityReferences = new LinkedList<>(this.request.getEntityReferences());
        entityReferences.add(this.request.getDestination());
        return getTargetWiki(entityReferences);
    }
}
