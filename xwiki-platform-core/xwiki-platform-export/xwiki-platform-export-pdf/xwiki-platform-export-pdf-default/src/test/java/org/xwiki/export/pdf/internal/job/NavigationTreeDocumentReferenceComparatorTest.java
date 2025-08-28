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
package org.xwiki.export.pdf.internal.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.tree.Tree;

/**
 * Unit tests for {@link NavigationTreeDocumentReferenceComparator}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({DefaultStringEntityReferenceSerializer.class, DefaultStringEntityReferenceResolver.class,
    DefaultSymbolScheme.class, DefaultEntityReferenceProvider.class, DefaultModelConfiguration.class})
class NavigationTreeDocumentReferenceComparatorTest
{
    @InjectMockComponents
    private NavigationTreeDocumentReferenceComparator pageHierarchyDocumentReferenceComparator;

    @Named("nestedPages")
    @MockComponent
    private Tree nestedPagesTree;

    private DocumentReference alice = new DocumentReference("test", List.of("Some", "User"), "Alice");

    private DocumentReference aliceNested = new DocumentReference("test", List.of("Some", "User", "Alice"), "WebHome");

    private DocumentReference aliceTopLevel = new DocumentReference("test", "Alice", "WebHome");

    private DocumentReference aliceDev = new DocumentReference("dev", List.of("Some", "User"), "Alice");

    private DocumentReference bob = new DocumentReference("test", List.of("Some", "User"), "Bob");

    private DocumentReference bobTopLevel = new DocumentReference("test", "Bob", "WebHome");

    private DocumentReference carol = new DocumentReference("test", List.of("Some", "User"), "Carol");

    private DocumentReference denisNested = new DocumentReference("test", List.of("Some", "User", "Denis"), "WebHome");

    @BeforeEach
    void configure()
    {
        when(this.nestedPagesTree.getProperties()).thenReturn(new HashMap<>());

        when(this.nestedPagesTree.getPath("document:test:Some.User.Alice")).thenReturn(List.of("farm:*", "wiki:test",
            "document:test:Some.WebHome", "document:test:Some.User.WebHome", "document:test:Some.User.Alice"));
        when(this.nestedPagesTree.getPath("document:test:Some.User.Alice.WebHome"))
            .thenReturn(List.of("farm:*", "wiki:test", "document:test:Some.WebHome", "document:test:Some.User.WebHome",
                "document:test:Some.User.Alice.WebHome"));
        when(this.nestedPagesTree.getPath("document:test:Alice.WebHome"))
            .thenReturn(List.of("farm:*", "wiki:test", "document:test:Alice.WebHome"));
        when(this.nestedPagesTree.getPath("document:dev:Some.User.Alice")).thenReturn(List.of("farm:*", "wiki:dev",
            "document:dev:Some.WebHome", "document:dev:Some.User.WebHome", "document:dev:Some.User.Alice"));
        when(this.nestedPagesTree.getPath("document:test:Some.User.Bob")).thenReturn(List.of("farm:*", "wiki:test",
            "document:test:Some.WebHome", "document:test:Some.User.WebHome", "document:test:Some.User.Bob"));
        when(this.nestedPagesTree.getPath("document:test:Bob.WebHome"))
            .thenReturn(List.of("farm:*", "wiki:test", "document:test:Bob.WebHome"));
        when(this.nestedPagesTree.getPath("document:test:Some.User.Carol")).thenReturn(List.of("farm:*", "wiki:test",
            "document:test:Some.WebHome", "document:test:Some.User.WebHome", "document:test:Some.User.Carol"));
        when(this.nestedPagesTree.getPath("document:test:Some.User.Denis.WebHome"))
            .thenReturn(List.of("farm:*", "wiki:test", "document:test:Some.WebHome", "document:test:Some.User.WebHome",
                "document:test:Some.User.Denis.WebHome"));

        when(this.nestedPagesTree.getParent("document:test:Some.User.Alice"))
            .thenReturn("document:test:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("document:test:Some.User.Alice.WebHome"))
            .thenReturn("document:test:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("document:test:Alice.WebHome")).thenReturn("wiki:test");
        when(this.nestedPagesTree.getParent("document:dev:Some.User.Alice"))
            .thenReturn("document:dev:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("document:test:Some.User.Bob"))
            .thenReturn("document:test:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("document:test:Bob.WebHome")).thenReturn("wiki:test");
        when(this.nestedPagesTree.getParent("document:test:Some.User.Carol"))
            .thenReturn("document:test:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("document:test:Some.User.Denis.WebHome"))
            .thenReturn("document:test:Some.User.WebHome");
        when(this.nestedPagesTree.getParent("wiki:test")).thenReturn("farm:*");
    }

    @Test
    void compareSameReference()
    {
        assertEquals(0, this.pageHierarchyDocumentReferenceComparator.compare(alice, alice));
        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(alice,
            new DocumentReference(alice, Locale.FRENCH)) < 0);
        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(new DocumentReference(alice, Locale.FRENCH),
            new DocumentReference(alice, Locale.ENGLISH)) > 0);

        verify(this.nestedPagesTree, never()).getChildren(any(), anyInt(), anyInt());
    }

    @Test
    void compareSameParent()
    {
        when(this.nestedPagesTree.getChildren("document:test:Some.User.WebHome", 0, 100))
            .thenReturn(List.of("document:test:Some.User.Carol"));

        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(alice, carol) > 0);
        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(carol, bob) < 0);

        when(this.nestedPagesTree.getChildren("document:test:Some.User.WebHome", 1, 100))
            .thenReturn(List.of("document:test:Some.User.Denis.WebHome"));
        when(this.nestedPagesTree.getChildren("document:test:Some.User.WebHome", 2, 100))
            .thenReturn(List.of("document:test:Some.User.Bob", "document:test:Some.User.Alice"));

        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(alice, bob) > 0);
        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(denisNested, bob) < 0);

        verify(this.nestedPagesTree, times(3)).getChildren(any(), anyInt(), anyInt());
    }

    @Test
    void compareNestedWithTerminal()
    {
        when(this.nestedPagesTree.getChildren("document:test:Some.User.WebHome", 0, 100))
            .thenReturn(List.of("document:test:Some.User.Alice.WebHome", "document:test:Some.User.Alice"));

        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(aliceNested, alice) < 0);
    }

    @Test
    void comparetNotFound()
    {
        // Falls back on the base comparator.
        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(alice, bob) < 0);
    }

    @Test
    void compareDifferentWiki()
    {
        when(this.nestedPagesTree.getChildren("farm:*", 0, 100)).thenReturn(List.of("wiki:test", "wiki:dev"));

        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(aliceDev, bob) > 0);
    }

    @Test
    void compareTopLevelPages()
    {

        when(this.nestedPagesTree.getChildren("wiki:test", 0, 100))
            .thenReturn(List.of("document:test:Bob.WebHome", "document:test:Alice.WebHome"));

        assertTrue(this.pageHierarchyDocumentReferenceComparator.compare(aliceTopLevel, bobTopLevel) > 0);
    }
}
