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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

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

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
    void setPinnedChildPagesForWiki() throws Exception
    {
        EntityReference wikiReference = new WikiReference("foo");
        List<DocumentReference> pinnedChildPages = List.of(
            new DocumentReference("xwiki", "Some", "Page"),
            new DocumentReference("foo", "Main", "WebHome"),
            new DocumentReference("xwiki", List.of("Some", "Sub"), "Page"),
            new DocumentReference("foo", "Test", "Page1")
        );
        DocumentReference storageReference = new DocumentReference("foo", "XWiki", "XWikiPreferences");
        when(this.xwiki.getDocument(storageReference, this.xcontext)).thenReturn(this.storageDocument);
        when(this.storageDocument.clone()).thenReturn(this.storageDocument);
        when(this.storageDocument.isNew()).thenReturn(true);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(this.storageDocument.getAuthors()).thenReturn(documentAuthors);

        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);

        this.pinnedChildPagesManager.setPinnedChildPages(wikiReference, pinnedChildPages);
        verify(this.storageDocument).setStringListValue(PinnedChildPagesClassInitializer.CLASS_REFERENCE,
            PinnedChildPagesClassInitializer.PROPERTY_NAME, List.of("Main/"));
        verify(this.storageDocument).setHidden(true);
        verify(this.storageDocument).clone();
        verify(documentAuthors).setOriginalMetadataAuthor(userReference);
        verify(this.xwiki).saveDocument(this.storageDocument, "Update pinned pages", true, this.xcontext);
        assertEquals(3, this.logCapture.size());
        assertEquals("Page [xwiki:Some.Page] is not a child of [Wiki foo] so it won't be pinned.",
            this.logCapture.getMessage(0));
        assertEquals("Page [xwiki:Some.Sub.Page] is not a child of [Wiki foo] so it won't be pinned.",
            this.logCapture.getMessage(1));
        assertEquals("Page [foo:Test.Page1] is not a child of [Wiki foo] so it won't be pinned.",
            this.logCapture.getMessage(2));
    }

    @Test
    void setPinnedChildPagesForSpace() throws Exception
    {
        EntityReference wikiReference = new SpaceReference("xwiki", "Some");
        List<DocumentReference> pinnedChildPages = List.of(
            new DocumentReference("xwiki", "Some", "Pa+ge"),
            new DocumentReference("foo", "Main", "WebHome"),
            new DocumentReference("xwiki", List.of("Some", "S%u/b"), "WebHome"),
            new DocumentReference("foo", "Test", "Page1")
        );
        DocumentReference storageReference = new DocumentReference("xwiki", "Some", "WebPreferences");
        when(this.xwiki.getDocument(storageReference, this.xcontext)).thenReturn(this.storageDocument);
        when(this.storageDocument.clone()).thenReturn(this.storageDocument);
        when(this.storageDocument.isNew()).thenReturn(false);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(this.storageDocument.getAuthors()).thenReturn(documentAuthors);

        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);

        this.pinnedChildPagesManager.setPinnedChildPages(wikiReference, pinnedChildPages);
        verify(this.storageDocument).setStringListValue(PinnedChildPagesClassInitializer.CLASS_REFERENCE,
            PinnedChildPagesClassInitializer.PROPERTY_NAME, List.of("Pa%2Bge", "S%25u%2Fb/"));
        verify(this.storageDocument, never()).setHidden(true);
        verify(this.storageDocument).clone();
        verify(documentAuthors).setOriginalMetadataAuthor(userReference);
        verify(this.xwiki).saveDocument(this.storageDocument, "Update pinned pages", true, this.xcontext);
        assertEquals(2, this.logCapture.size());
        assertEquals("Page [foo:Main.WebHome] is not a child of [Space xwiki:Some] so it won't be pinned.",
            this.logCapture.getMessage(0));
        assertEquals("Page [foo:Test.Page1] is not a child of [Space xwiki:Some] so it won't be pinned.",
            this.logCapture.getMessage(1));
    }

    @Test
    void setPinnedChildPagesForDocument() throws Exception
    {
        EntityReference wikiReference = new DocumentReference("xwiki", "Some", "Page");
        List<DocumentReference> pinnedChildPages = List.of(
            new DocumentReference("xwiki", "Some", "Page"),
            new DocumentReference("foo", "Main", "WebHome"),
            new DocumentReference("xwiki", List.of("Some", "Sub"), "WebHome"),
            new DocumentReference("foo", "Test", "Page1")
        );
        DocumentReference storageReference = new DocumentReference("xwiki", "Some", "WebPreferences");
        when(this.xwiki.getDocument(storageReference, this.xcontext)).thenReturn(this.storageDocument);
        when(this.storageDocument.clone()).thenReturn(this.storageDocument);
        when(this.storageDocument.isNew()).thenReturn(false);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(this.storageDocument.getAuthors()).thenReturn(documentAuthors);

        UserReference userReference = mock(UserReference.class);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userReference);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.pinnedChildPagesManager.setPinnedChildPages(wikiReference,
        pinnedChildPages));
        assertEquals("Invalid parent reference [xwiki:Some.Page], only nested document reference are allowed.",
            exception.getMessage());
    }
}
