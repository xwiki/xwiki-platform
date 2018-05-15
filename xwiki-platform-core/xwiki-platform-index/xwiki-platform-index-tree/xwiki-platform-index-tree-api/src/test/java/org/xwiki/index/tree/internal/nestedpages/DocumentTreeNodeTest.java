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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.tree.TreeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTreeNode}.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@ComponentTest
public class DocumentTreeNodeTest
{
    @InjectMockComponents
    private DocumentTreeNode documentTreeNode;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("translations")
    private TreeNode translationsTreeNode;

    @MockComponent
    @Named("attachments")
    private TreeNode attachmentsTreeNode;

    @MockComponent
    @Named("classProperties")
    private TreeNode classPropertiesTreeNode;

    @MockComponent
    @Named("objects")
    private TreeNode objectsTreeNode;

    @Mock
    @Named("nestedPagesOrderedByName")
    private Query nestedPagesOrderedByName;

    private DocumentReference documentReference =
        new DocumentReference("wiki", Arrays.asList("Path", "To", "Page"), "WebHome");

    @BeforeComponent
    public void configure(MockitoComponentManager componentManager) throws Exception
    {
        Provider<ComponentManager> contextComponentManagerProvider = componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(componentManager);
    }

    @BeforeEach
    public void before() throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.WebHome", EntityType.DOCUMENT))
            .thenReturn(documentReference);
        when(this.defaultEntityReferenceSerializer.serialize(documentReference))
            .thenReturn("wiki:Path.To.Page.WebHome");
        when(this.localEntityReferenceSerializer.serialize(documentReference.getParent())).thenReturn("Path.To.Page");
        when(this.queryManager.getNamedQuery("nestedPagesOrderedByName")).thenReturn(this.nestedPagesOrderedByName);
        when(this.nestedPagesOrderedByName.addFilter(any(QueryFilter.class))).thenReturn(this.nestedPagesOrderedByName);
    }

    /**
     * @see "XWIKI-14643: Missing page in breadcrumbs treeview when treeview is expanded"
     */
    @Test
    public void pagination() throws Exception
    {
        this.documentTreeNode.getProperties().put("hierarchyMode", "reference");
        this.documentTreeNode.getProperties().put("showTranslations", true);
        this.documentTreeNode.getProperties().put("showAttachments", true);
        this.documentTreeNode.getProperties().put("showClassProperties", true);
        this.documentTreeNode.getProperties().put("showObjects", true);
        this.documentTreeNode.getProperties().put("showAddDocument", true);

        when(this.authorization.hasAccess(Right.EDIT, documentReference.getParent())).thenReturn(true);
        when(this.translationsTreeNode.getChildCount("translations:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.classPropertiesTreeNode.getChildCount("classProperties:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.objectsTreeNode.getChildCount("objects:wiki:Path.To.Page.WebHome")).thenReturn(1);

        assertEquals(
            Arrays.asList("translations:wiki:Path.To.Page.WebHome", "attachments:wiki:Path.To.Page.WebHome",
                "classProperties:wiki:Path.To.Page.WebHome"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 0, 3));

        verify(this.nestedPagesOrderedByName, never()).execute();

        DocumentReference alice = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Alice");
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Collections.singletonList(alice));
        when(this.defaultEntityReferenceSerializer.serialize(alice)).thenReturn("wiki:Path.To.Page.Alice");

        assertEquals(
            Arrays.asList("objects:wiki:Path.To.Page.WebHome", "addDocument:wiki:Path.To.Page.WebHome",
                "document:wiki:Path.To.Page.Alice"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 3, 3));

        verify(this.nestedPagesOrderedByName).setOffset(0);
        verify(this.nestedPagesOrderedByName).setLimit(1);

        DocumentReference bob = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Bob");
        DocumentReference carol = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Carol");
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Arrays.asList(bob, carol));
        when(this.defaultEntityReferenceSerializer.serialize(bob)).thenReturn("wiki:Path.To.Page.Bob");
        when(this.defaultEntityReferenceSerializer.serialize(carol)).thenReturn("wiki:Path.To.Page.Carol");

        assertEquals(Arrays.asList("document:wiki:Path.To.Page.Bob", "document:wiki:Path.To.Page.Carol"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 6, 3));

        verify(this.nestedPagesOrderedByName).setOffset(1);
        verify(this.nestedPagesOrderedByName).setLimit(3);
    }

    @Test
    public void getChildrenByNameWithExclusions() throws Exception
    {
        this.documentTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("document:wiki:Path.To.OtherPage", "document:wiki:Path.To.Page.Alice",
                "document:wiki:Path.WebHome", "document:wiki:Path.To.Page.Bob.WebHome")));

        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.OtherPage", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("wiki", Arrays.asList("Path", "To"), "OtherPage"));
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.WebHome", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("wiki", "Path", "WebHome"));

        DocumentReference alice = new DocumentReference("Alice", this.documentReference.getLastSpaceReference());
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.Alice", EntityType.DOCUMENT))
            .thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice)).thenReturn("Path.To.Page.Alice");

        DocumentReference bob = new DocumentReference("wiki", Arrays.asList("Path", "To", "Page", "Bob"), "WebHome");
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.Bob.WebHome", EntityType.DOCUMENT))
            .thenReturn(bob);
        when(this.localEntityReferenceSerializer.serialize(bob.getParent())).thenReturn("Path.To.Page.Bob");

        DocumentReference child = new DocumentReference("Child", this.documentReference.getLastSpaceReference());
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Collections.singletonList(child));
        when(this.defaultEntityReferenceSerializer.serialize(child)).thenReturn("wiki:Path.To.Page.Child");

        assertEquals(Collections.singletonList("document:wiki:Path.To.Page.Child"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 0, 5));

        verify(this.nestedPagesOrderedByName).setWiki("wiki");
        verify(this.nestedPagesOrderedByName).bindValue("excludedDocuments",
            Collections.singleton("Path.To.Page.Alice"));
        verify(this.nestedPagesOrderedByName).bindValue("excludedSpaces", Collections.singleton("Path.To.Page.Bob"));
        verify(this.nestedPagesOrderedByName).bindValue("parent", "Path.To.Page");
    }

    @Test
    public void getChildCount() throws Exception
    {
        this.documentTreeNode.getProperties().put("exclusions",
            new HashSet<>(Arrays.asList("document:wiki:Path.To.Page.Alice", "document:wiki:Path.To.Page.Bob.WebHome")));

        DocumentReference alice = new DocumentReference("Alice", this.documentReference.getLastSpaceReference());
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.Alice", EntityType.DOCUMENT))
            .thenReturn(alice);
        when(this.localEntityReferenceSerializer.serialize(alice)).thenReturn("Path.To.Page.Alice");

        DocumentReference bob = new DocumentReference("wiki", Arrays.asList("Path", "To", "Page", "Bob"), "WebHome");
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.Bob.WebHome", EntityType.DOCUMENT))
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

        assertEquals(5L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        verify(childSpacesQuery).setWiki("wiki");
        verify(childSpacesQuery).bindValue("parent", "Path.To.Page");
        verify(childSpacesQuery).bindValue("excludedSpaces", Collections.singleton("Path.To.Page.Bob"));

        verify(childTerminalPagesQuery).setWiki("wiki");
        verify(childTerminalPagesQuery).bindValue("space", "Path.To.Page");
        verify(childTerminalPagesQuery).bindValue("defaultDocName", "WebHome");
        verify(childTerminalPagesQuery).bindValue("excludedDocuments", Collections.singleton("Path.To.Page.Alice"));
    }
}
