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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.tree.TreeNode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentTreeNode}.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
public class DocumentTreeNodeTest
{
    @Rule
    public MockitoComponentMockingRule<TreeNode> mocker = new MockitoComponentMockingRule<>(DocumentTreeNode.class);

    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    private EntityReferenceProvider defaultEntityReferenceProvider;

    private ContextualAuthorizationManager authorization;

    private Provider<ComponentManager> contextComponentManagerProvider;

    private QueryManager queryManager;

    private DocumentReference documentReference =
        new DocumentReference("wiki", Arrays.asList("Path", "To", "Page"), "WebHome");

    private TreeNode translationsTreeNode;

    private TreeNode attachmentsTreeNode;

    private TreeNode classPropertiesTreeNode;

    private TreeNode objectsTreeNode;

    private Query nestedPagesOrderedByName = mock(Query.class, "nestedPagesOrderedByName");

    @Before
    public void configure() throws Exception
    {
        this.currentEntityReferenceResolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        this.defaultEntityReferenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.defaultEntityReferenceProvider = this.mocker.getInstance(EntityReferenceProvider.class);
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        this.queryManager = this.mocker.getInstance(QueryManager.class);

        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
        when(this.currentEntityReferenceResolver.resolve("wiki:Path.To.Page.WebHome", EntityType.DOCUMENT))
            .thenReturn(documentReference);
        when(this.defaultEntityReferenceSerializer.serialize(documentReference))
            .thenReturn("wiki:Path.To.Page.WebHome");
        when(this.queryManager.getNamedQuery("nestedPagesOrderedByName")).thenReturn(this.nestedPagesOrderedByName);
        when(this.nestedPagesOrderedByName.addFilter(any(QueryFilter.class))).thenReturn(this.nestedPagesOrderedByName);

        this.contextComponentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(this.contextComponentManagerProvider.get()).thenReturn(this.mocker);

        this.translationsTreeNode = this.mocker.registerMockComponent(TreeNode.class, "translations");
        this.attachmentsTreeNode = this.mocker.registerMockComponent(TreeNode.class, "attachments");
        this.classPropertiesTreeNode = this.mocker.registerMockComponent(TreeNode.class, "classProperties");
        this.objectsTreeNode = this.mocker.registerMockComponent(TreeNode.class, "objects");
    }

    /**
     * @see "XWIKI-14643: Missing page in breadcrumbs treeview when treeview is expanded"
     */
    @Test
    public void pagination() throws Exception
    {
        TreeNode documentTreeNode = this.mocker.getComponentUnderTest();
        documentTreeNode.getProperties().put("hierarchyMode", "reference");
        documentTreeNode.getProperties().put("showTranslations", true);
        documentTreeNode.getProperties().put("showAttachments", true);
        documentTreeNode.getProperties().put("showClassProperties", true);
        documentTreeNode.getProperties().put("showObjects", true);
        documentTreeNode.getProperties().put("showAddDocument", true);

        when(this.authorization.hasAccess(Right.EDIT, documentReference.getParent())).thenReturn(true);
        when(this.translationsTreeNode.getChildCount("translations:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.classPropertiesTreeNode.getChildCount("classProperties:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.objectsTreeNode.getChildCount("objects:wiki:Path.To.Page.WebHome")).thenReturn(1);

        assertEquals(
            Arrays.asList("translations:wiki:Path.To.Page.WebHome", "attachments:wiki:Path.To.Page.WebHome",
                "classProperties:wiki:Path.To.Page.WebHome"),
            documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 0, 3));

        verify(this.nestedPagesOrderedByName, never()).execute();

        DocumentReference alice = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Alice");
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Collections.singletonList(alice));
        when(this.defaultEntityReferenceSerializer.serialize(alice)).thenReturn("wiki:Path.To.Page.Alice");

        assertEquals(
            Arrays.asList("objects:wiki:Path.To.Page.WebHome", "addDocument:wiki:Path.To.Page.WebHome",
                "document:wiki:Path.To.Page.Alice"),
            documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 3, 3));

        verify(this.nestedPagesOrderedByName).setOffset(0);
        verify(this.nestedPagesOrderedByName).setLimit(1);

        DocumentReference bob = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Bob");
        DocumentReference carol = new DocumentReference("wiki", Arrays.asList("Path.To.Page"), "Carol");
        when(this.nestedPagesOrderedByName.execute()).thenReturn(Arrays.asList(bob, carol));
        when(this.defaultEntityReferenceSerializer.serialize(bob)).thenReturn("wiki:Path.To.Page.Bob");
        when(this.defaultEntityReferenceSerializer.serialize(carol)).thenReturn("wiki:Path.To.Page.Carol");

        assertEquals(Arrays.asList("document:wiki:Path.To.Page.Bob", "document:wiki:Path.To.Page.Carol"),
            documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 6, 3));

        verify(this.nestedPagesOrderedByName).setOffset(1);
        verify(this.nestedPagesOrderedByName).setLimit(3);
    }
}
