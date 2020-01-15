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
package org.xwiki.model.reference;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Validate {@link EntityReferenceTree}.
 * 
 * @version $Id$
 */
public class EntityReferenceTreeTest
{
    @Test
    public void testOneReference()
    {
        EntityReferenceTreeNode treeNode = new EntityReferenceTree(new DocumentReference("wiki", "space", "page"));

        assertEquals(null, treeNode.getReference());
        assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("wiki");

        assertNotNull(treeNode);
        assertEquals(new WikiReference("wiki"), treeNode.getReference());
        assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("space");

        assertNotNull(treeNode);
        assertEquals(new SpaceReference("space", new WikiReference("wiki")), treeNode.getReference());
        assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("page");

        assertNotNull(treeNode);
        assertEquals(new DocumentReference("wiki", "space", "page"), treeNode.getReference());
        assertEquals(0, treeNode.getChildren().size());
    }

    @Test
    public void testSeveralReferences()
    {
        EntityReferenceTreeNode tree = new EntityReferenceTree(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space2", "page2"), new DocumentReference("wiki", "space", "page2"),
            new DocumentReference("wiki2", "space2", "page2"));

        assertEquals(null, tree.getReference());
        assertEquals(2, tree.getChildren().size());

        {
            EntityReferenceTreeNode treeNode = tree.get("wiki");

            assertNotNull(treeNode);
            assertEquals(new WikiReference("wiki"), treeNode.getReference());
            assertEquals(2, treeNode.getChildren().size());

            treeNode = treeNode.get("space");

            assertNotNull(treeNode);
            assertEquals(new SpaceReference("space", new WikiReference("wiki")), treeNode.getReference());
            assertEquals(2, treeNode.getChildren().size());

            treeNode = treeNode.get("page");

            assertNotNull(treeNode);
            assertEquals(new DocumentReference("wiki", "space", "page"), treeNode.getReference());
            assertEquals(0, treeNode.getChildren().size());
        }

        {
            EntityReferenceTreeNode treeNode = tree.get("wiki2");

            assertNotNull(treeNode);
            assertEquals(new WikiReference("wiki2"), treeNode.getReference());
            assertEquals(1, treeNode.getChildren().size());

            treeNode = treeNode.get("space2");

            assertNotNull(treeNode);
            assertEquals(new SpaceReference("space2", new WikiReference("wiki2")), treeNode.getReference());
            assertEquals(1, treeNode.getChildren().size());

            treeNode = treeNode.get("page2");

            assertNotNull(treeNode);
            assertEquals(new DocumentReference("wiki2", "space2", "page2"), treeNode.getReference());
            assertEquals(0, treeNode.getChildren().size());
        }
    }

    @Test
    public void getDescendantByReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        EntityReferenceTree tree = new EntityReferenceTree(documentReference);

        assertSame(documentReference.getWikiReference(), tree.get(new WikiReference("wiki")).getReference());

        assertSame(documentReference.getLastSpaceReference(),
            tree.get(documentReference.getLastSpaceReference()).getReference());

        EntityReference entityReference =
            new EntityReference(documentReference.getName(), EntityType.DOCUMENT, documentReference.getParent());
        assertSame(documentReference, tree.get(entityReference).getReference());

        // The entity type should be taken into account.
        assertNull(tree
            .get(new EntityReference(documentReference.getName(), EntityType.SPACE, documentReference.getParent())));

        assertNull(tree.get((EntityReference) null));

        assertNull(tree.get(new SpaceReference("From", documentReference.getParent().getParent())));
    }

    @Test
    public void testSiblingWithDifferentType()
    {
        DocumentReference document = new DocumentReference("wiki", "space", "entity");
        SpaceReference space = new SpaceReference("wiki", "space", "entity");

        EntityReferenceTreeNode tree = new EntityReferenceTree(document, space);

        assertSame(document, tree.get(document).getReference());
        assertSame(space, tree.get(space).getReference());

        EntityReferenceTreeNode treeNode = tree;

        assertEquals(null, treeNode.getReference());
        assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("wiki");

        assertNotNull(treeNode);
        assertSame(document.getWikiReference(), treeNode.getReference());
        assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("space");

        assertNotNull(treeNode);
        assertSame(document.getParent(), treeNode.getReference());
        assertEquals(2, treeNode.getChildren().size());
        assertEquals(1, treeNode.getChildren(EntityType.DOCUMENT).size());
        assertEquals(1, treeNode.getChildren(EntityType.SPACE).size());

        EntityReferenceTreeNode documentNode = treeNode.getChildren(EntityType.DOCUMENT).iterator().next();

        assertNotNull(documentNode);
        assertSame(document, documentNode.getReference());
        assertEquals(0, documentNode.getChildren().size());

        EntityReferenceTreeNode spaceNode = treeNode.getChildren(EntityType.SPACE).iterator().next();

        assertNotNull(spaceNode);
        assertSame(space, spaceNode.getReference());
        assertEquals(0, spaceNode.getChildren().size());
    }

    @Test
    public void testToString()
    {
        EntityReferenceTreeNode tree = new EntityReferenceTree(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space2", "page2"), new DocumentReference("wiki", "space", "page2"),
            new DocumentReference("wiki2", "space2", "page2"));

        assertEquals("[Wiki wiki = [" + "Space wiki:space = [wiki:space.page, wiki:space.page2],"
            + " Space wiki:space2 = [wiki:space2.page2]],"
            + " Wiki wiki2 = [Space wiki2:space2 = [wiki2:space2.page2]]]", tree.toString());
    }
}
