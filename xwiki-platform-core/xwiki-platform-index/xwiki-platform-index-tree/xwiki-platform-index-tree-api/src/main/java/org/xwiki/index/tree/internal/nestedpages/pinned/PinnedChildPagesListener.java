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
package org.xwiki.index.tree.internal.nestedpages.pinned;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Update the list of pinned child pages when a pinned page is deleted, moved or renamed.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Singleton
@Named("PinnedChildPagesListener")
public class PinnedChildPagesListener extends AbstractLocalEventListener
{
    @Inject
    private PinnedChildPagesManager pinnedChildPagesManager;

    @Inject
    private JobContext jobContext;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    /**
     * Default constructor.
     */
    public PinnedChildPagesListener()
    {
        super("Pinned child pages listener", new DocumentDeletedEvent(), new DocumentRenamedEvent());
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentRenamedEvent) {
            DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) event;
            onDocumentRenamed(documentRenamedEvent.getSourceReference(), documentRenamedEvent.getTargetReference());
        } else if (event instanceof DocumentDeletedEvent && RefactoringJobs.DELETE.equals(getCurrentJobType())) {
            // A delete event is triggered before each document rename event, but we don't want to remove the pinned
            // child page in this case because we won't be able to replace the pinned child page later when the rename
            // event is triggered. For this reason we have to check if this is really a delete and not a rename.
            onDocumentDeleted(((XWikiDocument) source).getDocumentReference());
        }
    }

    /**
     * When a document is deleted (not moved or renamed) we need to remove it from the list of pinned child pages of its
     * parent document.
     *
     * @param documentReference the reference of the document that was deleted
     */
    private void onDocumentDeleted(DocumentReference documentReference)
    {
        EntityReference parentReference = this.pinnedChildPagesManager.getParent(documentReference);
        List<DocumentReference> pinnedChildPages = getMutablePinnedChildPages(parentReference);
        if (pinnedChildPages.remove(documentReference)) {
            this.pinnedChildPagesManager.setPinnedChildPages(parentReference, pinnedChildPages);
        }
    }

    /**
     * When a document is renamed we have 3 cases:
     * <ul>
     * <li>the document is the explicit target of a rename or move job:
     * <ul>
     * <li>its parent doesn't change (i.e. the document is only renamed, not moved, which means the document is the
     * target of a rename job): in this case we need to rename the corresponding entry in the list of pinned child
     * pages</li>
     * <li>its parent changes (i.e. the document is moved but its parent remains in place): in this case we need to
     * remove the document from the list of pinned child pages of the old parent</li>
     * </ul>
     * </li>
     * <li>the document is moved / renamed as a side effect of renaming / moving one of its ancestors (which is the
     * actual target of the rename / move job): we don't have to do anything in this case because the pinned pages store
     * (i.e. the WebPreferences page) is moved to the new location as well</li>
     * </ul>
     *
     * @param oldReference the old reference of the document
     * @param newReference the new reference of the document
     */
    private void onDocumentRenamed(DocumentReference oldReference, DocumentReference newReference)
    {
        if (isRenameOrMoveJobTarget(oldReference)) {
            EntityReference oldParentReference = this.pinnedChildPagesManager.getParent(oldReference);
            EntityReference newParentReference = this.pinnedChildPagesManager.getParent(newReference);
            if (Objects.equals(oldParentReference, newParentReference)) {
                // The document is only renamed, not moved.
                List<DocumentReference> pinnedChildPages = getMutablePinnedChildPages(oldParentReference);
                int index = pinnedChildPages.indexOf(oldReference);
                if (index >= 0) {
                    pinnedChildPages.set(index, newReference);
                    this.pinnedChildPagesManager.setPinnedChildPages(oldParentReference, pinnedChildPages);
                }
            } else {
                // The document is moved without its parent.
                onDocumentDeleted(oldReference);
            }
        }
    }

    private List<DocumentReference> getMutablePinnedChildPages(EntityReference parentReference)
    {
        return new LinkedList<>(this.pinnedChildPagesManager.getPinnedChildPages(parentReference));
    }

    /**
     * Checks if the current refactoring job is a rename or move operation that targets explicitly the specified
     * document.
     *
     * @param documentReference the reference of the document before the rename / move operation
     * @return {@code true} if the specified document is the explicit target of the current rename or move job,
     *         {@code false} otherwise
     */
    private boolean isRenameOrMoveJobTarget(DocumentReference documentReference)
    {
        String currentJobType = getCurrentJobType();
        if (RefactoringJobs.RENAME.equals(currentJobType) || RefactoringJobs.MOVE.equals(currentJobType)) {
            Request request = this.jobContext.getCurrentJob().getRequest();
            if (request instanceof MoveRequest) {
                MoveRequest moveRequest = (MoveRequest) request;
                Collection<EntityReference> movedEntities = moveRequest.getEntityReferences();
                return contains(movedEntities, documentReference);
            }
        }
        return false;
    }

    private String getCurrentJobType()
    {
        Job job = this.jobContext.getCurrentJob();
        return job != null ? job.getType() : null;
    }

    private boolean contains(Collection<EntityReference> entityReferences, EntityReference entityReference)
    {
        if (!entityReferences.contains(entityReference) && EntityType.DOCUMENT.equals(entityReference.getType())) {
            // If the given entity reference is a reference of a space home page then look for the space reference as
            // well because the refactoring API accepts a space reference when you want to rename / move an entire
            // space.
            String spaceHomePageName =
                this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
            return spaceHomePageName.equals(entityReference.getName())
                && entityReferences.contains(entityReference.getParent());
        }
        return true;
    }
}
