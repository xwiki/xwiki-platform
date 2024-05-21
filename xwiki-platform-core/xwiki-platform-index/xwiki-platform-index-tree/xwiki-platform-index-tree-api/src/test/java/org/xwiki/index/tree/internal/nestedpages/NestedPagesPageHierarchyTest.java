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
package org.xwiki.index.tree.internal.nestedpages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.index.tree.PageHierarchy.ChildrenQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link NestedPagesPageHierarchy}.
 *
 * @version $Id$
 */
@ComponentTest
class NestedPagesPageHierarchyTest
{
    @InjectMockComponents
    private NestedPagesPageHierarchy nestedPagesPageHierarchy;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("count")
    private QueryFilter countFilter;

    @MockComponent
    @Named("document")
    private QueryFilter documenFilter;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Mock
    private Query query;

    @Mock
    private QueryParameter queryParameter;

    @BeforeEach
    void configure()
    {
        when(this.queryParameter.query()).thenReturn(this.query);
    }

    @Test
    void getTopLevelPages() throws Exception
    {
        WikiReference wikiReference = new WikiReference("test");
        DocumentReference alice = new DocumentReference("test", "Alice", "WebHome");
        DocumentReference bob = new DocumentReference("test", "Alice", "WebHome");

        when(this.queryManager
            .createQuery(", XWikiSpace AS space WHERE doc.space = space.reference AND doc.name = 'WebHome'"
                + " AND space.parent IS NULL AND space.name LIKE :text ORDER BY doc.fullName", Query.HQL))
                    .thenReturn(this.query);
        when(this.query.bindValue("text")).thenReturn(this.queryParameter);
        when(this.queryParameter.anyChars()).thenReturn(this.queryParameter);
        when(this.queryParameter.literal("foo")).thenReturn(this.queryParameter);
        when(this.query.setOffset(10)).thenReturn(this.query);
        when(this.query.setWiki("test")).thenReturn(this.query);
        when(this.query.addFilter(this.documenFilter)).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of(alice, bob));

        ChildrenQuery childrenQuery =
            this.nestedPagesPageHierarchy.getChildren(wikiReference).withOffset(10).matching("foo");

        assertEquals(List.of(alice, bob), childrenQuery.getDocumentReferences());
    }

    @Test
    void countLevelPages() throws Exception
    {
        WikiReference wikiReference = new WikiReference("test");

        when(this.queryManager
            .createQuery(", XWikiSpace AS space WHERE doc.space = space.reference AND doc.name = 'WebHome'"
                + " AND space.parent IS NULL", Query.HQL)).thenReturn(this.query);
        when(this.query.setWiki("test")).thenReturn(query);
        when(this.query.addFilter(this.countFilter)).thenReturn(query);
        when(this.query.execute()).thenReturn(List.of(7L));

        ChildrenQuery childrenQuery = this.nestedPagesPageHierarchy.getChildren(wikiReference);

        assertEquals(7, childrenQuery.count());
    }

    @Test
    void getChildrenOfANestedPage() throws Exception
    {
        DocumentReference parentReference = new DocumentReference("test", "Parent", "WebHome");
        DocumentReference alice = new DocumentReference("test", "Parent", "Alice");
        DocumentReference bob = new DocumentReference("test", List.of("Parent", "Bob"), "WebHome");

        when(this.queryManager.createQuery(", XWikiSpace AS space WHERE doc.space = space.reference"
            + " AND ((doc.name <> 'WebHome' AND doc.space = :parent)"
            + " OR (doc.name = 'WebHome' AND space.parent = :parent))"
            + " AND ((doc.name <> 'WebHome' AND doc.name like :text)"
            + " OR (doc.name = 'WebHome' AND space.name LIKE :text)) ORDER BY doc.fullName", Query.HQL))
                .thenReturn(this.query);
        when(this.localEntityReferenceSerializer.serialize(parentReference.getLastSpaceReference()))
            .thenReturn("Parent");
        when(this.query.bindValue("parent", "Parent")).thenReturn(this.query);
        when(this.query.bindValue("text")).thenReturn(this.queryParameter);
        when(this.queryParameter.anyChars()).thenReturn(this.queryParameter);
        when(this.queryParameter.literal("bar")).thenReturn(this.queryParameter);
        when(this.query.setLimit(5)).thenReturn(this.query);
        when(this.query.setWiki("test")).thenReturn(this.query);
        when(this.query.addFilter(this.documenFilter)).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of(alice, bob));

        ChildrenQuery childrenQuery =
            this.nestedPagesPageHierarchy.getChildren(parentReference).withLimit(5).matching("bar");

        assertEquals(List.of(alice, bob), childrenQuery.getDocumentReferences());
    }

    @Test
    void countChildrenOfANestedPage() throws Exception
    {
        DocumentReference parentReference = new DocumentReference("test", "Parent", "WebHome");

        when(this.queryManager.createQuery(", XWikiSpace AS space WHERE doc.space = space.reference"
            + " AND ((doc.name <> 'WebHome' AND doc.space = :parent)"
            + " OR (doc.name = 'WebHome' AND space.parent = :parent))", Query.HQL)).thenReturn(this.query);
        when(this.localEntityReferenceSerializer.serialize(parentReference.getLastSpaceReference()))
            .thenReturn("Parent");
        when(this.query.bindValue("parent", "Parent")).thenReturn(this.query);
        when(this.query.setWiki("test")).thenReturn(query);
        when(this.query.addFilter(this.countFilter)).thenReturn(query);
        when(this.query.execute()).thenReturn(List.of(3L));

        ChildrenQuery childrenQuery = this.nestedPagesPageHierarchy.getChildren(parentReference);

        assertEquals(3, childrenQuery.count());
    }

    @Test
    void getChildrenOfATerminalPage() throws Exception
    {
        DocumentReference parentReference = new DocumentReference("test", "Some", "Page");
        ChildrenQuery childrenQuery = this.nestedPagesPageHierarchy.getChildren(parentReference);

        assertEquals(0, childrenQuery.count());
        assertEquals(List.of(), childrenQuery.getDocumentReferences());

        verify(this.queryManager, never()).createQuery(anyString(), anyString());
    }
}
