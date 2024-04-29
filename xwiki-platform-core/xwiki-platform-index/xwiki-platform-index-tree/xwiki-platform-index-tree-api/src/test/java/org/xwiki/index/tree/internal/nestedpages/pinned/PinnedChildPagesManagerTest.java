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
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link PinnedChildPagesManager}.
 *
 * @version $Id$
 */
@ComponentTest
class PinnedChildPagesManagerTest
{
    @InjectMockComponents
    private PinnedChildPagesManager pinnedChildPagesManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private ContextualLocalizationManager contextLocalization;

    @Mock
    XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiDocument storageDocument;

    private DocumentReference pinnedChildPagesClassReference =
        new DocumentReference(PinnedChildPagesClassInitializer.CLASS_REFERENCE, new WikiReference("wiki"));

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.contextLocalization.getTranslationPlain("index.tree.pinnedChildPages.saveComment"))
            .thenReturn("Update pinned pages");
    }

    @Test
    void getParent()
    {
        DocumentReference documentReference = new DocumentReference("wiki", List.of("Path", "To"), "Page");
        assertEquals(documentReference.getLastSpaceReference(),
            this.pinnedChildPagesManager.getParent(documentReference));

        documentReference = new DocumentReference("wiki", List.of("Path", "To"), "WebHome");
        assertEquals(documentReference.getLastSpaceReference().getParent(),
            this.pinnedChildPagesManager.getParent(documentReference));

        documentReference = new DocumentReference("wiki", "Top", "WebHome");
        assertEquals(documentReference.getWikiReference(), this.pinnedChildPagesManager.getParent(documentReference));
    }

    @Test
    void getPinnedChildPages() throws Exception
    {
        when(this.storageDocument.getListValue(this.pinnedChildPagesClassReference, "pinnedChildPages"))
            .thenReturn(List.of("alice/", "bob"));

        // Get top level pinned pages.
        DocumentReference storageReference = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
        when(this.xwiki.getDocument(storageReference, this.xcontext)).thenReturn(this.storageDocument);
        assertEquals(
            List.of(new DocumentReference("wiki", "alice", "WebHome"), new DocumentReference("wiki", "bob", "WebHome")),
            this.pinnedChildPagesManager.getPinnedChildPages(this.pinnedChildPagesClassReference.getWikiReference()));

        // Get pinned pages for a space.
        storageReference = new DocumentReference("wiki", "space", "WebPreferences");
        when(this.xwiki.getDocument(storageReference, this.xcontext)).thenReturn(this.storageDocument);
        assertEquals(
            List.of(new DocumentReference("wiki", List.of("space", "alice"), "WebHome"),
                new DocumentReference("wiki", "space", "bob")),
            this.pinnedChildPagesManager.getPinnedChildPages(new DocumentReference("wiki", "space", "WebHome")));

        // Terminal pages can't have child pages so they can't have pinned child pages either.
        assertEquals(List.of(),
            this.pinnedChildPagesManager.getPinnedChildPages(new DocumentReference("wiki", "space", "page")));
    }

    @Test
    void setPinnedChildPages()
    {
        // TODO
    }
}
