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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.job.DeleteJob;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Update the list of pinned child pages when a pinned page is delete, moved or renamed.
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
        } else if (event instanceof DocumentDeletedEvent && this.jobContext.getCurrentJob() instanceof DeleteJob) {
            // A delete event is triggered before each document rename event, but we don't want to remove the pinned
            // child page in this case because we won't be able to replace the pinned child page later when the rename
            // event is triggered. For this reason we have to check if this is really a delete and not a rename.
            onDocumentDeleted(((XWikiDocument) source).getDocumentReference());
        }
    }

    private void onDocumentDeleted(DocumentReference documentReference)
    {
        EntityReference parentReference = this.pinnedChildPagesManager.getParent(documentReference);
        List<DocumentReference> pinnedChildPages = getMutablePinnedChildPages(parentReference);
        if (pinnedChildPages.remove(documentReference)) {
            this.pinnedChildPagesManager.setPinnedChildPages(parentReference, pinnedChildPages);
        }
    }

    private void onDocumentRenamed(DocumentReference oldReference, DocumentReference newReference)
    {
        EntityReference oldParentReference = this.pinnedChildPagesManager.getParent(oldReference);
        EntityReference newParentReference = this.pinnedChildPagesManager.getParent(newReference);
        if (Objects.equals(oldParentReference, newParentReference)) {
            List<DocumentReference> pinnedChildPages = getMutablePinnedChildPages(oldParentReference);
            int index = pinnedChildPages.indexOf(oldReference);
            if (index >= 0) {
                pinnedChildPages.set(index, newReference);
                this.pinnedChildPagesManager.setPinnedChildPages(oldParentReference, pinnedChildPages);
            }
        } else {
            onDocumentDeleted(oldReference);
        }
    }

    private List<DocumentReference> getMutablePinnedChildPages(EntityReference parentReference)
    {
        return new LinkedList<>(this.pinnedChildPagesManager.getPinnedChildPages(parentReference));
    }
}
