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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

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
 * @since 11.10RC1
 */
@ComponentTest
public class InstalledExtensionDocumentTreeTest
{
    @InjectMockComponents
    private InstalledExtensionDocumentTree tree;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    private DocumentReference path = new DocumentReference("foo", "Path", "WebHome");

    private DocumentReference to = new DocumentReference("foo", Arrays.asList("Path", "To"), "WebHome");

    private DocumentReference bob = new DocumentReference("foo", Arrays.asList("Path", "Bob"), "WebHome");

    private DocumentReference alice = new DocumentReference("foo", Arrays.asList("Path", "To"), "Alice");

    private DocumentReference carol = new DocumentReference("bar", "Carol", "WebHome");

    private DocumentReference john = new DocumentReference("bar", "Carol", "John");

    private DocumentReference users = new DocumentReference("bar", "Users", "WebHome");

    private DocumentReference denis = new DocumentReference("bar", "Users", "Denis");

    @BeforeEach
    public void configure()
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
    public void getChildren()
    {
        assertEquals(Collections.singleton(this.path), this.tree.getChildren(this.alice.getWikiReference()));
        assertEquals(new HashSet<>(Arrays.asList(this.to, this.bob)), this.tree.getChildren(path));
        assertEquals(Collections.singleton(alice), this.tree.getChildren(this.to));
        assertEquals(Collections.emptySet(), this.tree.getChildren(alice));
        assertEquals(Collections.emptySet(), this.tree.getChildren(bob));

        assertEquals(new HashSet<>(Arrays.asList(this.carol, this.users)),
            this.tree.getChildren(carol.getWikiReference()));
        assertEquals(Collections.singleton(this.john), this.tree.getChildren(carol));
        assertEquals(Collections.singleton(this.denis), this.tree.getChildren(users));
        assertEquals(Collections.emptySet(), this.tree.getChildren(denis));

        assertEquals(Collections.emptySet(), this.tree.getChildren(new WikiReference("test")));
        assertEquals(Collections.emptySet(), this.tree.getChildren(this.bob.getLastSpaceReference()));
        assertEquals(Collections.emptySet(),
            this.tree.getChildren(new DocumentReference("bar", Arrays.asList("Carol", "John"), "WebHome")));
        assertEquals(Collections.emptySet(), this.tree.getChildren(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    public void getNestedExtensionPages()
    {
        assertEquals(new HashSet<>(Arrays.asList(this.alice, this.bob)),
            this.tree.getNestedExtensionPages(this.alice.getWikiReference()));
        assertEquals(new HashSet<>(Arrays.asList(this.alice, this.bob)), this.tree.getNestedExtensionPages(this.path));
        assertEquals(Collections.singleton(this.alice), this.tree.getNestedExtensionPages(this.to));

        assertEquals(new HashSet<>(Arrays.asList(this.carol, this.denis, this.john)),
            this.tree.getNestedExtensionPages(this.carol.getWikiReference()));
        assertEquals(Collections.singleton(this.john), this.tree.getNestedExtensionPages(this.carol));
        assertEquals(Collections.singleton(this.denis), this.tree.getNestedExtensionPages(this.users));

        assertEquals(Collections.emptySet(), this.tree.getNestedExtensionPages(new WikiReference("test")));
        assertEquals(Collections.emptySet(), this.tree.getNestedExtensionPages(this.bob));
        assertEquals(Collections.emptySet(), this.tree.getNestedExtensionPages(this.path.getLastSpaceReference()));
        assertEquals(Collections.emptySet(),
            this.tree.getNestedExtensionPages(new DocumentReference("bar", "Path", "WebHome")));
        assertEquals(Collections.emptySet(),
            this.tree.getNestedExtensionPages(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    public void getNestedCustomizedExtensionPages()
    {
        this.tree.setCustomizedExtensionPage(this.alice, true);
        this.tree.setCustomizedExtensionPage(this.bob, false);
        this.tree.setCustomizedExtensionPage(new DocumentReference(this.denis, Locale.FRENCH), true);

        assertEquals(Collections.singleton(this.alice),
            this.tree.getNestedCustomizedExtensionPages(this.alice.getWikiReference()));
        assertEquals(Collections.singleton(this.alice), this.tree.getNestedCustomizedExtensionPages(this.path));

        assertEquals(Collections.singleton(this.denis),
            this.tree.getNestedCustomizedExtensionPages(this.carol.getWikiReference()));
        assertEquals(Collections.emptySet(), this.tree.getNestedCustomizedExtensionPages(this.carol));
        assertEquals(Collections.singleton(this.denis), this.tree.getNestedCustomizedExtensionPages(this.users));

        assertEquals(Collections.emptySet(), this.tree.getNestedCustomizedExtensionPages(new WikiReference("test")));
        assertEquals(Collections.emptySet(), this.tree.getNestedCustomizedExtensionPages(this.bob));
        assertEquals(Collections.emptySet(),
            this.tree.getNestedCustomizedExtensionPages(this.path.getLastSpaceReference()));
        assertEquals(Collections.emptySet(),
            this.tree.getNestedCustomizedExtensionPages(new DocumentReference("bar", "Path", "WebHome")));
        assertEquals(Collections.emptySet(),
            this.tree.getNestedCustomizedExtensionPages(new DocumentReference(this.path, Locale.FRENCH)));
    }

    @Test
    public void removeExtensionPage()
    {
        this.tree.removeExtensionPage(this.carol);
        assertEquals(new HashSet<>(Arrays.asList(this.carol, this.users)),
            this.tree.getChildren(carol.getWikiReference()));

        this.tree.removeExtensionPage(this.john);
        assertEquals(Collections.singleton(this.users), this.tree.getChildren(this.carol.getWikiReference()));
    }
}
