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
package org.xwiki.index.internal.tree.pinned;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultPinnedChildPagesResource}.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@ComponentTest
class DefaultPinnedChildPagesResourceTest
{
    @InjectMockComponents
    private DefaultPinnedChildPagesResource pinnedChildPagesResource;

    @MockComponent
    private PinnedChildPagesManager pinnedChildPagesManager;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @BeforeEach
    void setup()
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("xwiki", "Main", "WebHome"));
    }

    @Test
    void getPinnedChildPagesForWiki() throws XWikiRestException
    {
        String wikiName = "foo";
        String spaceName = "";
        DocumentReference ref1 = new DocumentReference("foo", "Main", "WebHome");
        DocumentReference ref2 = new DocumentReference("foo", "Test", "WebHome");
        List<DocumentReference> expectedReferences = List.of(ref1, ref2);
        when(this.pinnedChildPagesManager.getPinnedChildPages(new WikiReference(wikiName)))
            .thenReturn(expectedReferences);
        when(this.entityReferenceSerializer.serialize(ref1)).thenReturn("foo:Main.WebHome");
        when(this.entityReferenceSerializer.serialize(ref2)).thenReturn("foo:Test.WebHome");
        List<String> expectedResult = List.of("foo:Main.WebHome", "foo:Test.WebHome");

        assertEquals(expectedResult, this.pinnedChildPagesResource.getPinnedChildPages(wikiName, spaceName));
    }

    @Test
    void getPinnedChildPagesForSpace() throws XWikiRestException
    {
        String wikiName = "bar";
        String spaceName = "/spaces/Test/spaces/Foo/";
        DocumentReference spaceRef = new DocumentReference("bar", List.of("Test", "Foo"), "WebHome");

        DocumentReference ref1 = new DocumentReference("bar", List.of("Test", "Foo", "Space1"), "WebHome");
        DocumentReference ref2 = new DocumentReference("bar", List.of("Test", "Foo", "Space2"), "WebHome");
        List<DocumentReference> expectedReferences = List.of(ref1, ref2);
        when(this.pinnedChildPagesManager.getPinnedChildPages(spaceRef))
            .thenReturn(expectedReferences);
        when(this.entityReferenceSerializer.serialize(ref1)).thenReturn("bar:Test.Foo.Space1.WebHome");
        when(this.entityReferenceSerializer.serialize(ref2)).thenReturn("bar:Test.Foo.Space2.WebHome");
        List<String> expectedResult = List.of("bar:Test.Foo.Space1.WebHome", "bar:Test.Foo.Space2.WebHome");

        assertEquals(expectedResult, this.pinnedChildPagesResource.getPinnedChildPages(wikiName, spaceName));
    }

    @Test
    void setPinnedChildPagesForWiki() throws XWikiRestException
    {
        String wikiName = "foo";
        String spaceName = "";
        List<String> pinnedPages = List.of("foo:Main.WebHome", "foo:Test.WebHome");
        DocumentReference ref1 = new DocumentReference("foo", "Main", "WebHome");
        DocumentReference ref2 = new DocumentReference("foo", "Test", "WebHome");

        when(this.documentReferenceResolver.resolve("foo:Main.WebHome")).thenReturn(ref1);
        when(this.documentReferenceResolver.resolve("foo:Test.WebHome")).thenReturn(ref2);
        when(this.authorizationManager.hasAccess(Right.ADMIN, new WikiReference(wikiName))).thenReturn(false);
        Response response = this.pinnedChildPagesResource.setPinnedChildPages(wikiName, spaceName, pinnedPages);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(this.pinnedChildPagesManager, never()).setPinnedChildPages(any(), any());

        when(this.authorizationManager.hasAccess(Right.ADMIN, new WikiReference(wikiName))).thenReturn(true);
        response = this.pinnedChildPagesResource.setPinnedChildPages(wikiName, spaceName, pinnedPages);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(this.pinnedChildPagesManager).setPinnedChildPages(new WikiReference("foo"), List.of(ref1, ref2));
    }

    @Test
    void setPinnedChildPagesForSpace() throws XWikiRestException
    {
        String wikiName = "bar";
        String spaceName = "/spaces/Test/spaces/Foo/";
        DocumentReference spaceRef = new DocumentReference("bar", List.of("Test", "Foo"), "WebHome");
        List<String> pinnedPages = List.of("bar:Test.Foo.Space1.WebHome", "bar:Test.Foo.Space2.WebHome");
        DocumentReference ref1 = new DocumentReference("bar", List.of("Test", "Foo", "Space1"), "WebHome");
        DocumentReference ref2 = new DocumentReference("bar", List.of("Test", "Foo", "Space2"), "WebHome");

        when(this.documentReferenceResolver.resolve("bar:Test.Foo.Space1.WebHome")).thenReturn(ref1);
        when(this.documentReferenceResolver.resolve("bar:Test.Foo.Space2.WebHome")).thenReturn(ref2);
        when(this.authorizationManager.hasAccess(Right.EDIT, spaceRef)).thenReturn(false);
        Response response = this.pinnedChildPagesResource.setPinnedChildPages(wikiName, spaceName, pinnedPages);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        verify(this.pinnedChildPagesManager, never()).setPinnedChildPages(any(), any());

        when(this.authorizationManager.hasAccess(Right.EDIT, spaceRef)).thenReturn(true);
        response = this.pinnedChildPagesResource.setPinnedChildPages(wikiName, spaceName, pinnedPages);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        verify(this.pinnedChildPagesManager).setPinnedChildPages(spaceRef, List.of(ref1, ref2));
    }
}
