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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.tree.TreeFilter;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;

/**
 * Unit tests for {@link ChildDocumentsTreeNodeGroup}.
 *
 * @version $Id$
 */
@ComponentTest
class ChildDocumentsTreeNodeGroupTest
{
    @InjectMockComponents
    private ChildDocumentsTreeNodeGroup childDocumentsTreeNodeGroup;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private LocalizationContext localizationContext;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    @Named("childPage/nestedPages")
    private QueryFilter childPageFilter;

    @MockComponent
    @Named("documentReferenceResolver/nestedPages")
    private QueryFilter documentReferenceResolverFilter;

    @MockComponent
    @Named("topLevelPage/nestedPages")
    private QueryFilter topLevelPageFilter;

    @MockComponent
    @Named("hiddenPage/nestedPages")
    private QueryFilter hiddenPageFilter;

    @MockComponent
    @Named("excludedSpace/nestedPages")
    private QueryFilter excludedSpaceFilter;

    @MockComponent
    private QueryManager queryManager;

    @Mock
    @Named("nestedPagesOrderedByName")
    private Query nestedPagesOrderedByName;

    @MockComponent
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @MockComponent
    @Named("test")
    private TreeFilter filter;

    @Mock
    private Query query;

    private DocumentReference documentReference =
        new DocumentReference("wiki", List.of("Path", "To", "Page"), "WebHome");

    private DocumentReference terminalDocumentReference = new DocumentReference("wiki", "Some", "Page");

    @BeforeEach
    public void before(MockitoComponentManager componentManager) throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.WebHome"))
            .thenReturn(this.documentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, this.documentReference.getParent()))
            .thenReturn("space:wiki:Path.To.Page");
        when(this.localEntityReferenceSerializer.serialize(this.documentReference.getParent()))
            .thenReturn("Path.To.Page");

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Some.Page"))
            .thenReturn(this.terminalDocumentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, this.terminalDocumentReference.getParent()))
            .thenReturn("space:wiki:Some");

        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "wiki:foo"))
            .thenReturn(new WikiReference("foo"));

        DocumentReference alice = new DocumentReference("bar", "A", "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:bar:A.WebHome")).thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice.getParent())).thenReturn("A");

        DocumentReference bob = new DocumentReference("foo", "B", "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:foo:B.WebHome")).thenReturn(bob);
        when(this.localEntityReferenceSerializer.serialize(bob.getParent())).thenReturn("B");

        when(this.entityTreeNodeIdConverter.convert(String.class, new DocumentReference("foo", "C", "WebHome")))
            .thenReturn("document:foo:C.WebHome");

        when(this.query.addFilter(any(QueryFilter.class))).thenReturn(this.query);

        when(this.queryManager.getNamedQuery("nestedPagesOrderedByName")).thenReturn(this.nestedPagesOrderedByName);
        when(this.nestedPagesOrderedByName.addFilter(any(QueryFilter.class))).thenReturn(this.nestedPagesOrderedByName);

        when(this.contextComponentManagerProvider.get()).thenReturn(componentManager);
    }

    @Test
    void getChildDocuments() throws Exception
    {
        assertEquals(Collections.emptyList(),
            this.childDocumentsTreeNodeGroup.getChildDocuments(terminalDocumentReference, 0, 10));

        this.childDocumentsTreeNodeGroup.getProperties().put("showTerminalDocuments", false);
        Query queryNonTerminalPagesByName = mock(Query.class, "nonTerminalPagesOrderedByTitle");
        String statement = "select reference, 0 as terminal from XWikiSpace page order by lower(name), name";
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(queryNonTerminalPagesByName);
        when(queryNonTerminalPagesByName.addFilter(this.documentReferenceResolverFilter))
            .thenReturn(queryNonTerminalPagesByName);
        DocumentReference childReference = new DocumentReference("wiki", List.of("Path.To.Page"), "Alice");
        when(queryNonTerminalPagesByName.execute()).thenReturn(Collections.singletonList(childReference));

        assertEquals(Collections.singletonList(childReference),
            this.childDocumentsTreeNodeGroup.getChildDocuments(documentReference, 5, 3));

        verify(queryNonTerminalPagesByName).setWiki("wiki");
        verify(queryNonTerminalPagesByName).setOffset(5);
        verify(queryNonTerminalPagesByName).setLimit(3);
        verify(queryNonTerminalPagesByName).addFilter(this.childPageFilter);
        verify(queryNonTerminalPagesByName).bindValue("parent", "Path.To.Page");

        this.childDocumentsTreeNodeGroup.getProperties().put("orderBy", "title");
        Query queryNonTerminalPagesByTitle = mock(Query.class, "nonTerminalPagesOrderedByTitle");
        when(this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle"))
            .thenReturn(queryNonTerminalPagesByTitle);
        childReference = new DocumentReference("wiki", List.of("Path.To.Page"), "Bob");
        when(queryNonTerminalPagesByTitle.addFilter(this.documentReferenceResolverFilter))
            .thenReturn(queryNonTerminalPagesByTitle);
        when(queryNonTerminalPagesByTitle.execute()).thenReturn(Collections.singletonList(childReference));
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.GERMAN);

        assertEquals(Collections.singletonList(childReference),
            this.childDocumentsTreeNodeGroup.getChildDocuments(documentReference, 0, 5));

        verify(queryNonTerminalPagesByTitle).bindValue("locale", "de");
    }

    @Test
    void getChildrenByNameWithExclusions() throws Exception
    {
        this.childDocumentsTreeNodeGroup.getProperties().put("exclusions",
            new HashSet<>(List.of("document:wiki:Path.To.OtherPage", "document:wiki:Path.To.Page.Alice",
                "document:wiki:Path.WebHome", "document:wiki:Path.To.Page.Bob.WebHome")));

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.OtherPage"))
            .thenReturn(new DocumentReference("wiki", List.of("Path", "To"), "OtherPage"));
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.WebHome"))
            .thenReturn(new DocumentReference("wiki", "Path", "WebHome"));

        DocumentReference alice = new DocumentReference("Alice", this.documentReference.getLastSpaceReference());
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.Alice"))
            .thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice)).thenReturn("Path.To.Page.Alice");

        DocumentReference bob = new DocumentReference("wiki", List.of("Path", "To", "Page", "Bob"), "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.Bob.WebHome"))
            .thenReturn(bob);
        when(this.localEntityReferenceSerializer.serialize(bob.getParent())).thenReturn("Path.To.Page.Bob");

        this.childDocumentsTreeNodeGroup.getProperties().put("filters", Collections.singletonList("test"));
        when(this.entityTreeNodeIdConverter.convert(String.class, alice.getParent()))
            .thenReturn("space:wiki:Path.To.Page");
        when(this.filter.getChildExclusions("space:wiki:Path.To.Page")).thenReturn(
            new HashSet<>(List.of("document:wiki:Path.To.Page.John.WebHome", "document:wiki:Path.To.Page.Oliver")));

        DocumentReference john = new DocumentReference("wiki", List.of("Path", "To", "Page", "John"), "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.John.WebHome"))
            .thenReturn(john);
        when(this.localEntityReferenceSerializer.serialize(john.getLastSpaceReference()))
            .thenReturn("Path.To.Page.John");

        DocumentReference oliver = new DocumentReference("wiki", List.of("Path", "To", "Page"), "Oliver");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.Oliver"))
            .thenReturn(oliver);
        when(this.localEntityReferenceSerializer.serialize(oliver)).thenReturn("Path.To.Page.Oliver");

        DocumentReference child = new DocumentReference("Child", this.documentReference.getLastSpaceReference());
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Collections.singletonList(child));
        when(this.entityTreeNodeIdConverter.convert(String.class, child))
            .thenReturn("document:wiki:Path.To.Page.Child");

        assertEquals(Collections.singletonList("document:wiki:Path.To.Page.Child"),
            this.childDocumentsTreeNodeGroup.getChildren("document:wiki:Path.To.Page.WebHome", 0, 5));

        verify(this.nestedPagesOrderedByName).bindValue("excludedDocuments",
            new HashSet<>(List.of("Path.To.Page.Alice", "Path.To.Page.Oliver")));
        verify(this.nestedPagesOrderedByName).bindValue("excludedSpaces",
            new HashSet<>(List.of("Path.To.Page.Bob", "Path.To.Page.John")));
    }

    @Test
    void getChildCount() throws Exception
    {
        this.childDocumentsTreeNodeGroup.getProperties().put("exclusions",
            new HashSet<>(List.of("document:wiki:Path.To.Page.Alice", "document:wiki:Path.To.Page.Bob.WebHome")));

        DocumentReference alice = new DocumentReference("Alice", this.documentReference.getLastSpaceReference());
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.Alice"))
            .thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice)).thenReturn("Path.To.Page.Alice");

        DocumentReference bob = new DocumentReference("wiki", List.of("Path", "To", "Page", "Bob"), "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.Bob.WebHome"))
            .thenReturn(bob);
        when(this.localEntityReferenceSerializer.serialize(bob.getParent())).thenReturn("Path.To.Page.Bob");

        Query childSpacesQuery = mock(Query.class, "childSpaces");
        when(this.queryManager.createQuery(
            "select count(*) from XWikiSpace where parent = :parent " + "and reference not in (:excludedSpaces)",
            Query.HQL)).thenReturn(childSpacesQuery);
        when(childSpacesQuery.execute()).thenReturn(Collections.singletonList(2L));

        Query childTerminalPagesQuery = mock(Query.class, "childTerminalPages");
        when(this.queryManager.createQuery("where doc.translation = 0 and doc.space = :space and "
            + "doc.name <> :defaultDocName and doc.fullName not in (:excludedDocuments)", Query.HQL))
                .thenReturn(childTerminalPagesQuery);
        when(childTerminalPagesQuery.execute()).thenReturn(Collections.singletonList(3L));

        assertEquals(5L, this.childDocumentsTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome"));

        verify(childSpacesQuery).setWiki("wiki");
        verify(childSpacesQuery).bindValue("parent", "Path.To.Page");
        verify(childSpacesQuery).bindValue("excludedSpaces", Collections.singleton("Path.To.Page.Bob"));

        verify(childTerminalPagesQuery).setWiki("wiki");
        verify(childTerminalPagesQuery).bindValue("space", "Path.To.Page");
        verify(childTerminalPagesQuery).bindValue("defaultDocName", "WebHome");
        verify(childTerminalPagesQuery).bindValue("excludedDocuments", Collections.singleton("Path.To.Page.Alice"));

        this.childDocumentsTreeNodeGroup.getProperties().put("showTerminalDocuments", false);

        assertEquals(2L, this.childDocumentsTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome"));
    }

    @Test
    void getChildCountForWiki() throws Exception
    {
        // Filter hidden child nodes.
        this.childDocumentsTreeNodeGroup.getProperties().put("filterHiddenDocuments", true);
        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);

        assertEquals(0, this.childDocumentsTreeNodeGroup.getChildCount("something"));
        assertEquals(0, this.childDocumentsTreeNodeGroup.getChildCount("some:thing"));

        when(this.queryManager.createQuery("select count(*) from XWikiSpace where parent is null and hidden <> true",
            Query.HQL)).thenReturn(this.query);
        when(query.execute()).thenReturn(Collections.singletonList(2L));

        assertEquals(2L, this.childDocumentsTreeNodeGroup.getChildCount("wiki:foo"));

        verify(this.query).setWiki("foo");
        verify(this.query, never()).bindValue(anyString(), any());
    }

    @Test
    void getChildCountForWikiWithExclusions() throws Exception
    {
        this.childDocumentsTreeNodeGroup.getProperties().put("exclusions", new HashSet<>(
            List.of("document:bar:A.WebHome", "document:foo:B.WebHome", "document:foo:C.D", "space:foo:C")));

        DocumentReference denis = new DocumentReference("foo", "C", "D");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:foo:C.D")).thenReturn(denis);

        SpaceReference carol = new SpaceReference("foo", "C");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "space:foo:C")).thenReturn(carol);
        when(this.localEntityReferenceSerializer.serialize(carol)).thenReturn("C");

        this.childDocumentsTreeNodeGroup.getProperties().put("filters", Collections.singletonList("test"));
        when(this.entityTreeNodeIdConverter.convert(String.class, new WikiReference("foo"))).thenReturn("wiki:foo");
        when(this.filter.getChildExclusions("wiki:foo")).thenReturn(Collections.singleton("document:foo:J.WebHome"));

        DocumentReference john = new DocumentReference("foo", "J", "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:foo:J.WebHome")).thenReturn(john);
        when(this.localEntityReferenceSerializer.serialize(john.getLastSpaceReference())).thenReturn("J");

        when(this.queryManager.createQuery(
            "select count(*) from XWikiSpace where parent is null " + "and reference not in (:excludedSpaces)",
            Query.HQL)).thenReturn(this.query);
        when(query.execute()).thenReturn(Collections.singletonList(2L));

        assertEquals(2L, this.childDocumentsTreeNodeGroup.getChildCount("wiki:foo"));

        verify(this.query).setWiki("foo");
        verify(this.query).bindValue("excludedSpaces", new HashSet<String>(List.of("B", "C", "J")));
    }

    @Test
    void getChildrenForWiki() throws Exception
    {
        // Filter hidden child nodes.
        this.childDocumentsTreeNodeGroup.getProperties().put("filterHiddenDocuments", true);
        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);

        String statement = "select reference, 0 as terminal from XWikiSpace page order by lower(name), name";
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(this.query);
        when(query.execute()).thenReturn(Collections.singletonList(new DocumentReference("foo", "C", "WebHome")));

        assertEquals(Collections.singletonList("document:foo:C.WebHome"),
            this.childDocumentsTreeNodeGroup.getChildren("wiki:foo", 5, 10));

        verify(this.query).setWiki("foo");
        verify(this.query).setOffset(5);
        verify(this.query).setLimit(10);
        verify(this.query).addFilter(this.topLevelPageFilter);
        verify(this.query).addFilter(this.hiddenPageFilter);
    }

    @Test
    void getChildrenForWikiByTitle() throws Exception
    {
        // Don't filter hidden child nodes.
        this.childDocumentsTreeNodeGroup.getProperties().put("filterHiddenDocuments", true);
        UserProperties userProperties = mock(UserProperties.class);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        when(this.userPropertiesResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(userProperties);

        this.childDocumentsTreeNodeGroup.getProperties().put("orderBy", "title");
        this.childDocumentsTreeNodeGroup.getProperties().put("exclusions",
            new HashSet<>(List.of("document:bar:A.WebHome", "document:foo:B.WebHome")));

        when(this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle")).thenReturn(this.query);
        when(this.localizationContext.getCurrentLocale()).thenReturn(Locale.FRENCH);
        when(query.execute()).thenReturn(Collections.singletonList(new DocumentReference("foo", "C", "WebHome")));

        assertEquals(Collections.singletonList("document:foo:C.WebHome"),
            this.childDocumentsTreeNodeGroup.getChildren("wiki:foo", 5, 10));

        verify(this.query).setWiki("foo");
        verify(this.query).setOffset(5);
        verify(this.query).setLimit(10);
        verify(this.query).bindValue("locale", "fr");
        verify(this.query).bindValue("excludedSpaces", Collections.singleton("B"));
        verify(this.query).addFilter(this.excludedSpaceFilter);
    }
}
