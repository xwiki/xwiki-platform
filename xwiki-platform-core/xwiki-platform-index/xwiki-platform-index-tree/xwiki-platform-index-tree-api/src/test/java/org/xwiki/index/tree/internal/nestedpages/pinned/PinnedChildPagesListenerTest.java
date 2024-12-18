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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link PinnedChildPagesListener}.
 *
 * @version $Id$
 */
@ComponentTest
class PinnedChildPagesListenerTest
{
    @InjectMockComponents
    private PinnedChildPagesListener pinnedChildPagesListener;

    @MockComponent
    private PinnedChildPagesManager pinnedChildPagesManager;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Mock
    private Job currentJob;

    @Mock
    private MoveRequest moveRequest;

    @Mock
    private XWikiDocument document;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    @Captor
    private ArgumentCaptor<EntityReference> parentReferenceCaptor = ArgumentCaptor.forClass(EntityReference.class);

    @Captor
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<DocumentReference>> pinnedPagesCaptor = ArgumentCaptor.forClass(List.class);

    @BeforeEach
    void configure()
    {
        when(this.document.getDocumentReference()).thenReturn(this.documentReference);
        when(this.pinnedChildPagesManager.getParent(this.documentReference))
            .thenReturn(this.documentReference.getLastSpaceReference());

        when(this.jobContext.getCurrentJob()).thenReturn(this.currentJob);
        when(this.currentJob.getRequest()).thenReturn(this.moveRequest);

        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
    }

    @Test
    void onDocumentDeleted()
    {
        // No pinned pages.
        this.pinnedChildPagesListener.onEvent(new DocumentDeletedEvent(), this.document, null);

        DocumentReference foo = new DocumentReference("wiki", "space", "foo");
        DocumentReference bar = new DocumentReference("wiki", "space", "bar");
        when(this.pinnedChildPagesManager.getPinnedChildPages(this.documentReference.getLastSpaceReference()))
            .thenReturn(List.of(foo, this.documentReference, bar));

        // Not inside a delete job (e.g. could be a rename job).
        this.pinnedChildPagesListener.onEvent(new DocumentDeletedEvent(), this.document, null);

        // Inside a delete job and there are pinned pages.
        when(this.currentJob.getType()).thenReturn(RefactoringJobs.DELETE);
        this.pinnedChildPagesListener.onEvent(new DocumentDeletedEvent(), this.document, null);

        // Trigger the event again to verify that the pinned pages are not updated again.
        when(this.pinnedChildPagesManager.getPinnedChildPages(this.documentReference.getLastSpaceReference()))
            .thenReturn(List.of(foo, bar));
        this.pinnedChildPagesListener.onEvent(new DocumentDeletedEvent(), this.document, null);

        verify(this.pinnedChildPagesManager).setPinnedChildPages(this.parentReferenceCaptor.capture(),
            this.pinnedPagesCaptor.capture());
        assertEquals(this.documentReference.getLastSpaceReference(), this.parentReferenceCaptor.getValue());
        assertEquals(List.of(foo, bar), this.pinnedPagesCaptor.getValue());
    }

    @Test
    void onDocumentRenamed()
    {
        DocumentReference foo = new DocumentReference("wiki", "space", "foo");
        DocumentReference bar = new DocumentReference("wiki", "space", "bar");
        when(this.pinnedChildPagesManager.getParent(foo)).thenReturn(foo.getLastSpaceReference());

        when(this.pinnedChildPagesManager.getPinnedChildPages(this.documentReference.getLastSpaceReference()))
            .thenReturn(List.of(foo, this.documentReference, bar));

        // Moving the page to a different parent should remove it from the pinned pages.
        DocumentReference targetReference = new DocumentReference("wiki", "other", "foo");
        when(this.pinnedChildPagesManager.getParent(targetReference))
            .thenReturn(targetReference.getLastSpaceReference());
        when(this.currentJob.getType()).thenReturn(RefactoringJobs.MOVE);
        when(this.moveRequest.getEntityReferences()).thenReturn(List.of(foo));

        this.pinnedChildPagesListener.onEvent(new DocumentRenamedEvent(foo, targetReference), null, null);

        // Moving the page along with its parent should not update the pinned pages.
        when(this.moveRequest.getEntityReferences()).thenReturn(List.of(foo.getLastSpaceReference()));
        this.pinnedChildPagesListener.onEvent(new DocumentRenamedEvent(foo, targetReference), null, null);

        // Renaming the page should update the pinned pages.
        when(this.pinnedChildPagesManager.getPinnedChildPages(this.documentReference.getLastSpaceReference()))
            .thenReturn(List.of(this.documentReference, bar));
        targetReference = new DocumentReference("wiki", "space", "otherPage");
        when(this.pinnedChildPagesManager.getParent(targetReference))
            .thenReturn(targetReference.getLastSpaceReference());
        when(this.currentJob.getType()).thenReturn(RefactoringJobs.RENAME);
        when(this.moveRequest.getEntityReferences()).thenReturn(List.of(this.documentReference));

        this.pinnedChildPagesListener.onEvent(new DocumentRenamedEvent(this.documentReference, targetReference), null,
            null);

        // Renaming the page along with its parent should not update the pinned pages.
        when(this.moveRequest.getEntityReferences())
            .thenReturn(List.of(this.documentReference.getLastSpaceReference()));
        this.pinnedChildPagesListener.onEvent(new DocumentRenamedEvent(this.documentReference, targetReference), null,
            null);

        // Renaming a page that is not pinned should not update the pinned pages. Let's trigger again the last rename.
        when(this.pinnedChildPagesManager.getPinnedChildPages(this.documentReference.getLastSpaceReference()))
            .thenReturn(List.of(targetReference, bar));
        this.pinnedChildPagesListener.onEvent(new DocumentRenamedEvent(this.documentReference, targetReference), null,
            null);

        verify(this.pinnedChildPagesManager, times(2)).setPinnedChildPages(this.parentReferenceCaptor.capture(),
            this.pinnedPagesCaptor.capture());
        assertEquals(List.of(foo.getLastSpaceReference(), this.documentReference.getLastSpaceReference()),
            this.parentReferenceCaptor.getAllValues());
        assertEquals(List.of(List.of(this.documentReference, bar), List.of(targetReference, bar)),
            this.pinnedPagesCaptor.getAllValues());
    }
}
