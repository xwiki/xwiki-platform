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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManager;
import org.xwiki.index.tree.internal.nestedpages.pinned.PinnedChildPagesTreeNodeGroup;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.tree.TreeNode;
import org.xwiki.tree.internal.DefaultCompositeTreeNodeGroup;

/**
 * Unit tests for {@link DocumentTreeNode}.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@ComponentTest
@ComponentList({DefaultCompositeTreeNodeGroup.class, ContextComponentManager.class})
class DocumentTreeNodeTest
{
    @InjectMockComponents
    private DocumentTreeNode documentTreeNode;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    private ContextualAuthorizationManager authorization;

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

    @MockComponent
    @Named("addDocument")
    private TreeNode addDocumentTreeNode;

    @MockComponent(classToMock = PinnedChildPagesTreeNodeGroup.class)
    @Named("pinnedChildPages")
    private TreeNode pinnedChildPagesTreeNodeGroup;

    @MockComponent(classToMock = ChildDocumentsTreeNodeGroup.class)
    @Named("childDocuments")
    private TreeNode childDocumentsTreeNodeGroup;

    @MockComponent
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    private DocumentReference documentReference =
        new DocumentReference("wiki", Arrays.asList("Path", "To", "Page"), "WebHome");

    private DocumentReference terminalDocumentReference = new DocumentReference("wiki", "Some", "Page");

    @BeforeEach
    public void before() throws Exception
    {
        when(this.translationsTreeNode.getType()).thenReturn("translations");
        when(this.attachmentsTreeNode.getType()).thenReturn("attachments");
        when(this.classPropertiesTreeNode.getType()).thenReturn("classProperties");
        when(this.objectsTreeNode.getType()).thenReturn("objects");
        when(this.addDocumentTreeNode.getType()).thenReturn("addDocument");

        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page.WebHome"))
            .thenReturn(this.documentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, this.documentReference.getParent()))
            .thenReturn("space:wiki:Path.To.Page");
        when(this.defaultEntityReferenceSerializer.serialize(this.documentReference))
            .thenReturn("wiki:Path.To.Page.WebHome");

        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Some.Page"))
            .thenReturn(this.terminalDocumentReference);
        when(this.entityTreeNodeIdConverter.convert(String.class, this.terminalDocumentReference.getParent()))
            .thenReturn("space:wiki:Some");
        when(this.defaultEntityReferenceSerializer.serialize(this.terminalDocumentReference))
            .thenReturn("wiki:Some.Page");
    }

    /**
     * @see "XWIKI-14643: Missing page in breadcrumbs treeview when treeview is expanded"
     */
    @Test
    void pagination() throws Exception
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

        when(this.childDocumentsTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome")).thenReturn(3);
        when(this.childDocumentsTreeNodeGroup.getChildren("document:wiki:Path.To.Page.WebHome", 0, 1))
            .thenReturn(List.of("document:wiki:Path.To.Page.Alice"));
        assertEquals(
            Arrays.asList("objects:wiki:Path.To.Page.WebHome", "addDocument:wiki:Path.To.Page.WebHome",
                "document:wiki:Path.To.Page.Alice"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 3, 3));

        when(this.childDocumentsTreeNodeGroup.getChildren("document:wiki:Path.To.Page.WebHome", 1, 3))
            .thenReturn(List.of("document:wiki:Path.To.Page.Bob", "document:wiki:Path.To.Page.Carol"));
        assertEquals(Arrays.asList("document:wiki:Path.To.Page.Bob", "document:wiki:Path.To.Page.Carol"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 6, 3));

        when(this.pinnedChildPagesTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome")).thenReturn(2);
        when(this.pinnedChildPagesTreeNodeGroup.getChildren("document:wiki:Path.To.Page.WebHome", 1, 3))
            .thenReturn(List.of("document:wiki:Path.To.Page.Pinned2"));
        when(this.childDocumentsTreeNodeGroup.getChildren("document:wiki:Path.To.Page.WebHome", 0, 2))
            .thenReturn(List.of("document:wiki:Path.To.Page.Alice", "document:wiki:Path.To.Page.Bob"));
        assertEquals(
            Arrays.asList("document:wiki:Path.To.Page.Pinned2", "document:wiki:Path.To.Page.Alice",
                "document:wiki:Path.To.Page.Bob"),
            this.documentTreeNode.getChildren("document:wiki:Path.To.Page.WebHome", 6, 3));
    }

    @Test
    void getChildCountWithTerminalPage()
    {
        when(this.translationsTreeNode.getChildCount("translations:wiki:Some.Page")).thenReturn(2);
        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Some.Page")).thenReturn(1);
        when(this.classPropertiesTreeNode.getChildCount("classProperties:wiki:Some.Page")).thenReturn(3);
        when(this.objectsTreeNode.getChildCount("objects:wiki:Some.Page")).thenReturn(5);

        assertEquals(0L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        this.documentTreeNode.getProperties().put("showTranslations", true);
        assertEquals(1L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        this.documentTreeNode.getProperties().put("showAttachments", true);
        assertEquals(2L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        this.documentTreeNode.getProperties().put("showClassProperties", true);
        assertEquals(3L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        this.documentTreeNode.getProperties().put("showObjects", true);
        assertEquals(4L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Some.Page")).thenReturn(0);
        assertEquals(3L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));

        when(this.pinnedChildPagesTreeNodeGroup.getChildCount("document:wiki:Some.Page")).thenReturn(3);
        when(this.childDocumentsTreeNodeGroup.getChildCount("document:wiki:Some.Page")).thenReturn(2);
        assertEquals(3L, this.documentTreeNode.getChildCount("document:wiki:Some.Page"));
    }

    @Test
    void getChildCountWithNestedPage()
    {
        when(this.translationsTreeNode.getChildCount("translations:wiki:Path.To.Page.WebHome")).thenReturn(2);
        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Path.To.Page.WebHome")).thenReturn(1);
        when(this.classPropertiesTreeNode.getChildCount("classProperties:wiki:Path.To.Page.WebHome")).thenReturn(3);
        when(this.objectsTreeNode.getChildCount("objects:wiki:Path.To.Page.WebHome")).thenReturn(5);

        assertEquals(0L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        this.documentTreeNode.getProperties().put("showTranslations", true);
        assertEquals(1L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        this.documentTreeNode.getProperties().put("showAttachments", true);
        assertEquals(2L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        this.documentTreeNode.getProperties().put("showClassProperties", true);
        assertEquals(3L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        this.documentTreeNode.getProperties().put("showObjects", true);
        assertEquals(4L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        when(this.attachmentsTreeNode.getChildCount("attachments:wiki:Path.To.Page.WebHome")).thenReturn(0);
        assertEquals(3L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        when(this.pinnedChildPagesTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome")).thenReturn(3);
        assertEquals(6L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));

        when(this.childDocumentsTreeNodeGroup.getChildCount("document:wiki:Path.To.Page.WebHome")).thenReturn(2);
        assertEquals(8L, this.documentTreeNode.getChildCount("document:wiki:Path.To.Page.WebHome"));
    }

    @Test
    void getParentForTerminalPage()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.Page"))
            .thenReturn(documentReference);

        when(this.entityTreeNodeIdConverter.convert(String.class,
            new DocumentReference("WebHome", documentReference.getLastSpaceReference())))
                .thenReturn("document:wiki:Path.To.WebHome");

        assertEquals("document:wiki:Path.To.WebHome", this.documentTreeNode.getParent("document:wiki:Path.To.Page"));
    }

    @Test
    void getParentForNestedPage()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.To.WebHome"))
            .thenReturn(documentReference);

        when(this.entityTreeNodeIdConverter.convert(String.class, new DocumentReference("wiki", "Path", "WebHome")))
            .thenReturn("document:wiki:Path.WebHome");

        assertEquals("document:wiki:Path.WebHome", this.documentTreeNode.getParent("document:wiki:Path.To.WebHome"));
    }

    @Test
    void getParentForTopLevelPage()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Path", "WebHome");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "document:wiki:Path.WebHome"))
            .thenReturn(documentReference);

        when(this.entityTreeNodeIdConverter.convert(String.class, new WikiReference("wiki"))).thenReturn("wiki:wiki");

        assertEquals("wiki:wiki", this.documentTreeNode.getParent("document:wiki:Path.WebHome"));
    }

    @Test
    void getParentForInvalidNode()
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Path");
        when(this.entityTreeNodeIdConverter.convert(EntityReference.class, "space:wiki:Path"))
            .thenReturn(spaceReference);

        assertEquals(null, this.documentTreeNode.getParent("space:wiki:Path"));
    }
}
