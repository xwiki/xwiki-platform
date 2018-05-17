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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiTreeNode}.
 * 
 * @version $Id$
 */
@ComponentTest
public class WikiTreeNodeTest
{
    @InjectMockComponents
    private WikiTreeNode wikiTreeNode;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private LocalizationContext localizationContext;

    @MockComponent
    private QueryManager queryManager;

    @Mock
    private Query query;

    @BeforeEach
    public void before()
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.currentEntityReferenceResolver.resolve("foo", EntityType.WIKI)).thenReturn(new WikiReference("foo"));

        DocumentReference alice = new DocumentReference("bar", "A", "WebHome");
        when(this.currentEntityReferenceResolver.resolve("bar:A.WebHome", EntityType.DOCUMENT)).thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice.getParent())).thenReturn("A");

        DocumentReference bob = new DocumentReference("foo", "B", "WebHome");
        when(this.currentEntityReferenceResolver.resolve("foo:B.WebHome", EntityType.DOCUMENT)).thenReturn(bob);
        when(this.localEntityReferenceSerializer.serialize(bob.getParent())).thenReturn("B");

        when(this.defaultEntityReferenceSerializer.serialize(new DocumentReference("foo", "C", "WebHome")))
            .thenReturn("foo:C.WebHome");

        when(this.query.addFilter(any(QueryFilter.class))).thenReturn(this.query);
    }

    @Test
    public void getParent()
    {
        assertEquals("farm:*", this.wikiTreeNode.getParent("wiki:foo"));
    }

    @Test
    public void getChildCount() throws Exception
    {
        assertEquals(0, this.wikiTreeNode.getChildCount("something"));
        assertEquals(0, this.wikiTreeNode.getChildCount("some:thing"));

        when(this.queryManager.createQuery("select count(*) from XWikiSpace where parent is null", Query.HQL))
            .thenReturn(this.query);
        when(query.execute()).thenReturn(Collections.singletonList(2L));

        assertEquals(2L, this.wikiTreeNode.getChildCount("wiki:foo"));

        verify(this.query).setWiki("foo");
        verify(this.query, never()).bindValue(anyString(), any());
    }

    @Test
    public void getChildCountWithExclusions() throws Exception
    {
        this.wikiTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("document:bar:A.WebHome", "document:foo:B.WebHome")));

        when(this.queryManager.createQuery(
            "select count(*) from XWikiSpace where parent is null " + "and reference not in (:excludedSpaces)",
            Query.HQL)).thenReturn(this.query);
        when(query.execute()).thenReturn(Collections.singletonList(2L));

        assertEquals(2L, this.wikiTreeNode.getChildCount("wiki:foo"));

        verify(this.query).setWiki("foo");
        verify(this.query).bindValue("excludedSpaces", Collections.singleton("B"));
    }

    @Test
    public void getChildrenByTitle() throws Exception
    {
        this.wikiTreeNode.getProperties().put("orderBy", "title");
        this.wikiTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("document:bar:A.WebHome", "document:foo:B.WebHome")));

        when(this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle")).thenReturn(this.query);
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.FRENCH);
        when(query.execute()).thenReturn(Collections.singletonList(new DocumentReference("foo", "C", "WebHome")));

        assertEquals(Collections.singletonList("document:foo:C.WebHome"),
            this.wikiTreeNode.getChildren("wiki:foo", 5, 10));

        verify(this.query).bindValue("locale", "fr");
        verify(this.query).bindValue("excludedSpaces", Collections.singleton("B"));
        verify(this.query).setWiki("foo");
        verify(this.query).setOffset(5);
        verify(this.query).setLimit(10);
    }
}
