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

import javax.inject.Inject;

import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.Request;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.EntityReferenceTreeNode;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Abstract job that targets multiple entities.
 * 
 * @param <R> the request type
 * @param <S> the job status type
 * @version $Id$
 * @since 7.2M1
 */
public abstract class AbstractEntityJob<R extends EntityRequest, S extends EntityJobStatus<? super R>> extends
    AbstractJob<R, S> implements GroupedJob
{
    /**
     * Generic interface used to implement the Visitor pattern.
     * 
     * @param <T> the type of nodes that are visited
     * @version $Id$
     */
    public interface Visitor<T>
    {
        /**
         * Visit the given node.
         * 
         * @param node the node to visit
         */
        void visit(T node);
    }

    private static final JobGroupPath ROOT_GROUP = new JobGroupPath(RefactoringJobs.GROUP, null);

    private static final String PREFERENCES_DOCUMENT_NAME = "WebPreferences";

    /**
     * The component used to access the XWiki model and to perform low level operations on it.
     */
    @Inject
    protected ModelBridge modelBridge;

    /**
     * Specifies the group this job is part of. If all the entities involved in this operation are from the same wiki
     * then this job is part of a group of refactoring jobs that run on that wiki (it will only block the refactoring
     * jobs that run on that wiki). Otherwise, if there are at least two entities from different wikis then this job is
     * part of the group of refactoring jobs that run at farm level.
     */
    private JobGroupPath groupPath;

    /**
     * Used to check access permissions.
     * 
     * @see #hasAccess(Right, EntityReference)
     */
    @Inject
    private AuthorizationManager authorization;

    /**
     * Used to distinguish the space home page.
     * 
     * @see #isSpaceHomeReference(DocumentReference)
     */
    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    public JobGroupPath getGroupPath()
    {
        return this.groupPath;
    }

    @Override
    public void initialize(Request request)
    {
        super.initialize(request);

        // Build the job group path.
        String targetWiki = getTargetWiki();
        if (targetWiki != null) {
            this.groupPath = new JobGroupPath(targetWiki, ROOT_GROUP);
        } else {
            this.groupPath = ROOT_GROUP;
        }
    }

    @Override
    protected void runInternal() throws Exception
    {
        Collection<EntityReference> entityReferences = this.request.getEntityReferences();
        if (entityReferences != null) {
            // Set the context user before executing the job. We don't have to restore the previous context user when
            // the job is finished because jobs are normally executed in a separate thread, with a separate execution
            // context.
            this.modelBridge.setContextUserReference(this.request.getUserReference());
            process(entityReferences);
        }
    }

    protected void process(Collection<EntityReference> entityReferences)
    {
        this.progressManager.pushLevelProgress(entityReferences.size(), this);

        try {
            for (EntityReference entityReference : entityReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    process(entityReference);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    /**
     * Process the specified entity.
     * 
     * @param entityReference the entity to process
     */
    protected abstract void process(EntityReference entityReference);

    /**
     * @return the wiki where the operation takes place, or {@code null} if the operation is cross wiki
     */
    protected String getTargetWiki()
    {
        return getTargetWiki(this.request.getEntityReferences());
    }

    /**
     * @return the wiki name if all the given entity references are from the same wiki, {@code null} otherwise
     */
    protected String getTargetWiki(Collection<EntityReference> entityReferences)
    {
        if (entityReferences == null) {
            return null;
        }

        String targetWiki = null;
        for (EntityReference entityReference : entityReferences) {
            EntityReference wikiReference = entityReference.extractReference(EntityType.WIKI);
            if (wikiReference != null) {
                if (targetWiki == null) {
                    targetWiki = wikiReference.getName();
                } else if (!targetWiki.equals(wikiReference.getName())) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return targetWiki;
    }

    protected boolean hasAccess(Right right, EntityReference reference)
    {
        return !this.request.isCheckRights()
            || this.authorization.hasAccess(right, this.request.getUserReference(), reference);
    }

    protected boolean isSpaceHomeReference(DocumentReference documentReference)
    {
        return documentReference.getName().equals(
            this.defaultEntityReferenceProvider.getDefaultReference(documentReference.getType()).getName());
    }

    private boolean isSpacePreferencesReference(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.DOCUMENT
            && PREFERENCES_DOCUMENT_NAME.equals(entityReference.getName());
    }

    protected void visitDocuments(SpaceReference spaceReference, Visitor<DocumentReference> visitor)
    {
        visitDocumentNodes(getDocumentReferenceTree(spaceReference), visitor);
    }

    private EntityReferenceTreeNode getDocumentReferenceTree(SpaceReference spaceReference)
    {
        return new EntityReferenceTree(this.modelBridge.getDocumentReferences(spaceReference)).get(spaceReference);
    }

    private void visitDocumentNodes(EntityReferenceTreeNode node, Visitor<DocumentReference> visitor)
    {
        EntityReference nodeReference = node.getReference();
        EntityType nodeType = nodeReference != null ? nodeReference.getType() : null;
        if (nodeType == EntityType.SPACE || nodeType == EntityType.WIKI || nodeType == null) {
            // A node that corresponds to an entity that can contain documents.
            visitDocumentAncestor(node, visitor);
        } else if (nodeType == EntityType.DOCUMENT) {
            visitor.visit((DocumentReference) node.getReference());
        }
    }

    private void visitDocumentAncestor(EntityReferenceTreeNode node, Visitor<DocumentReference> visitor)
    {
        Collection<EntityReferenceTreeNode> children = node.getChildren();
        this.progressManager.pushLevelProgress(children.size(), this);

        try {
            // Visit the space preferences document at the end as otherwise we may loose the space access rights.
            EntityReferenceTreeNode spacePreferencesNode = null;
            for (EntityReferenceTreeNode child : children) {
                if (isSpacePreferencesReference(child.getReference())) {
                    spacePreferencesNode = child;
                    continue;
                }
                visitDocumentAncestorStep(child, visitor);
            }

            if (spacePreferencesNode != null) {
                visitDocumentAncestorStep(spacePreferencesNode, visitor);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void visitDocumentAncestorStep(EntityReferenceTreeNode node, Visitor<DocumentReference> visitor)
    {
        this.progressManager.startStep(this);
        if (!this.status.isCanceled()) {
            visitDocumentNodes(node, visitor);
        }
    }
}
