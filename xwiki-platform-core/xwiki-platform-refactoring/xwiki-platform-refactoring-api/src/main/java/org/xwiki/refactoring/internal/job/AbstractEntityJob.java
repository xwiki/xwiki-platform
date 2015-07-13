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

import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.Request;
import org.xwiki.job.internal.AbstractJob;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceTreeNode;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.EntityRequest;
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

    private static final JobGroupPath ROOT_GROUP = new JobGroupPath("refactoring", null);

    private static final String PREFERENCES_DOCUMENT_NAME = "WebPreferences";

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
     * Used to distinguish between terminal and non-terminal (WebHome) documents.
     * 
     * @see #isTerminalDocument(DocumentReference)
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
        if (entityReferences == null) {
            return;
        }

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

    private boolean hasDefaultName(EntityReference entityReference)
    {
        return entityReference.getName().equals(
            this.defaultEntityReferenceProvider.getDefaultReference(entityReference.getType()).getName());
    }

    protected boolean isTerminalDocument(DocumentReference documentReference)
    {
        return !hasDefaultName(documentReference);
    }

    protected boolean isNestedDocument(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.DOCUMENT && hasDefaultName(entityReference);
    }

    protected boolean isPreferencesDocument(EntityReference entityReference)
    {
        return entityReference.getType() == EntityType.DOCUMENT
            && PREFERENCES_DOCUMENT_NAME.equals(entityReference.getName());
    }

    protected DocumentReference getPreferencesDocumentReference(DocumentReference documentReference)
    {
        return new DocumentReference(PREFERENCES_DOCUMENT_NAME, documentReference.getLastSpaceReference());
    }

    protected void visitNestedDocuments(EntityReferenceTreeNode node, Visitor<DocumentReference> visitor)
    {
        EntityType nodeType = node.getReference().getType();
        if (nodeType == EntityType.SPACE || nodeType == EntityType.WIKI) {
            // A node that can have nested documents.
            Collection<EntityReferenceTreeNode> children = node.getChildren();
            this.progressManager.pushLevelProgress(children.size(), this);

            try {
                // Visit the descendant documents first, and then the WebHome & WebPreferences documents on this level.
                EntityReferenceTreeNode webHomeNode = null;
                EntityReferenceTreeNode webPreferencesNode = null;
                for (EntityReferenceTreeNode child : children) {
                    if (isNestedDocument(child.getReference())) {
                        webHomeNode = child;
                        continue;
                    } else if (isPreferencesDocument(child.getReference())) {
                        webPreferencesNode = child;
                        continue;
                    }
                    visitNestedDocumentsStep(child, visitor);
                }

                if (webHomeNode != null) {
                    visitNestedDocumentsStep(webHomeNode, visitor);
                }

                if (webPreferencesNode != null) {
                    visitNestedDocumentsStep(webPreferencesNode, visitor);
                }
            } finally {
                this.progressManager.popLevelProgress(this);
            }
        } else if (nodeType == EntityType.DOCUMENT) {
            visitor.visit((DocumentReference) node.getReference());
        }
    }

    private void visitNestedDocumentsStep(EntityReferenceTreeNode node, Visitor<DocumentReference> visitor)
    {
        this.progressManager.startStep(this);
        if (!this.status.isCanceled()) {
            visitNestedDocuments(node, visitor);
        }
    }
}
