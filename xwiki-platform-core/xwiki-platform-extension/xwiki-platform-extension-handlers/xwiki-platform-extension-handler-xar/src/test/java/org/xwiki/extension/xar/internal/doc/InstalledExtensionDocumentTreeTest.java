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
package org.xwiki.extension.xar.internal.doc;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InstalledExtensionDocumentTree}.
 * 
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
class InstalledExtensionDocumentTreeTest
{
    @InjectMockComponents
    private InstalledExtensionDocumentTree tree;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    private DocumentReference path = new DocumentReference("foo", "Path", "WebHome");

    private DocumentReference to = new DocumentReference("foo", List.of("Path", "To"), "WebHome");

    private DocumentReference bob = new DocumentReference("foo", List.of("Path", "Bob"), "WebHome");

    private DocumentReference alice = new DocumentReference("foo", List.of("Path", "To"), "Alice");

    private DocumentReference carol = new DocumentReference("bar", "Carol", "WebHome");

    private DocumentReference john = new DocumentReference("bar", "Carol", "John");

    private DocumentReference users = new DocumentReference("bar", "Users", "WebHome");

    private DocumentReference denis = new DocumentReference("bar", "Users", "Denis");

    @BeforeEach
    void configure()
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));

        this.tree.addExtensionPage(alice);
        this.tree.addExtensionPage(bob);
        this.tree.addExtensionPage(denis);
        this.tree.addExtensionPage(john);
        this.tree.addExtensionPage(carol);
    }

    @Test
    void getChildren()
    {
        assertEquals(Set.of(this.path), this.tree.getChildren(this.alice.getWikiReference()));
        assertEquals(Set.of(this.to, this.bob), this.tree.getChildren(this.path));
        assertEquals(Set.of(this.alice), this.tree.getChildren(this.to));
        assertEquals(Set.of(), this.tree.getChildren(this.alice));
        assertEquals(Set.of(), this.tree.getChildren(this.bob));

        assertEquals(Set.of(this.carol, this.users), this.tree.getChildren(this.carol.getWikiReference()));
        assertEquals(Set.of(this.john), this.tree.getChildren(this.carol));
        assertEquals(Set.of(this.denis), this.tree.getChildren(this.users));
        assertEquals(Set.of(), this.tree.getChildren(this.denis));

        assertEquals(Set.of(), this.tree.getChildren(new WikiReference("test")));
        assertEquals(Set.of(), this.tree.getChildren(this.bob.getLastSpaceReference()));
        assertEquals(Set.of(),
            this.tree.getChildren(new DocumentReference("bar", List.of("Carol", "John"), "WebHome")));
        assertEquals(Set.of(), this.tree.getChildren(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    void getNestedExtensionPages()
    {
        assertEquals(Set.of(this.alice, this.bob),
            this.tree.getNestedExtensionPages(this.alice.getWikiReference()));
        assertEquals(Set.of(this.alice, this.bob), this.tree.getNestedExtensionPages(this.path));
        assertEquals(Set.of(this.alice), this.tree.getNestedExtensionPages(this.to));

        assertEquals(Set.of(this.carol, this.denis, this.john),
            this.tree.getNestedExtensionPages(this.carol.getWikiReference()));
        assertEquals(Set.of(this.john), this.tree.getNestedExtensionPages(this.carol));
        assertEquals(Set.of(this.denis), this.tree.getNestedExtensionPages(this.users));

        assertEquals(Set.of(), this.tree.getNestedExtensionPages(new WikiReference("test")));
        assertEquals(Set.of(), this.tree.getNestedExtensionPages(this.bob));
        assertEquals(Set.of(), this.tree.getNestedExtensionPages(this.path.getLastSpaceReference()));
        assertEquals(Set.of(),
            this.tree.getNestedExtensionPages(new DocumentReference("bar", "Path", "WebHome")));
        assertEquals(Set.of(),
            this.tree.getNestedExtensionPages(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    void getNestedCustomizedExtensionPages()
    {
        this.tree.setCustomizedExtensionPage(this.alice, true);
        this.tree.setCustomizedExtensionPage(this.bob, false);
        this.tree.setCustomizedExtensionPage(new DocumentReference(this.denis, Locale.FRENCH), true);

        assertEquals(Set.of(this.alice),
            this.tree.getNestedCustomizedExtensionPages(this.alice.getWikiReference()));
        assertEquals(Set.of(this.alice), this.tree.getNestedCustomizedExtensionPages(this.path));

        assertEquals(Set.of(this.denis),
            this.tree.getNestedCustomizedExtensionPages(this.carol.getWikiReference()));
        assertEquals(Set.of(), this.tree.getNestedCustomizedExtensionPages(this.carol));
        assertEquals(Set.of(this.denis), this.tree.getNestedCustomizedExtensionPages(this.users));

        assertEquals(Set.of(), this.tree.getNestedCustomizedExtensionPages(new WikiReference("test")));
        assertEquals(Set.of(), this.tree.getNestedCustomizedExtensionPages(this.bob));
        assertEquals(Set.of(),
            this.tree.getNestedCustomizedExtensionPages(this.path.getLastSpaceReference()));
        assertEquals(Set.of(),
            this.tree.getNestedCustomizedExtensionPages(new DocumentReference("bar", "Path", "WebHome")));
        assertEquals(Set.of(),
            this.tree.getNestedCustomizedExtensionPages(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    void removeExtensionPage()
    {
        this.tree.removeExtensionPage(this.carol);
        assertEquals(Set.of(this.carol, this.users), this.tree.getChildren(this.carol.getWikiReference()));

        this.tree.removeExtensionPage(this.john);
        assertEquals(Set.of(this.users), this.tree.getChildren(this.carol.getWikiReference()));
    }
}
