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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.EntityType;

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

        Assert.assertEquals(null, treeNode.getReference());
        Assert.assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("wiki");

        Assert.assertNotNull(treeNode);
        Assert.assertEquals(new WikiReference("wiki"), treeNode.getReference());
        Assert.assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("space");

        Assert.assertNotNull(treeNode);
        Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")), treeNode.getReference());
        Assert.assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("page");

        Assert.assertNotNull(treeNode);
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"), treeNode.getReference());
        Assert.assertEquals(0, treeNode.getChildren().size());
    }

    @Test
    public void testSeveralReferences()
    {
        EntityReferenceTreeNode tree = new EntityReferenceTree(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space2", "page2"), new DocumentReference("wiki", "space", "page2"),
            new DocumentReference("wiki2", "space2", "page2"));

        Assert.assertEquals(null, tree.getReference());
        Assert.assertEquals(2, tree.getChildren().size());

        {
            EntityReferenceTreeNode treeNode = tree.get("wiki");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new WikiReference("wiki"), treeNode.getReference());
            Assert.assertEquals(2, treeNode.getChildren().size());

            treeNode = treeNode.get("space");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")), treeNode.getReference());
            Assert.assertEquals(2, treeNode.getChildren().size());

            treeNode = treeNode.get("page");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new DocumentReference("wiki", "space", "page"), treeNode.getReference());
            Assert.assertEquals(0, treeNode.getChildren().size());
        }

        {
            EntityReferenceTreeNode treeNode = tree.get("wiki2");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new WikiReference("wiki2"), treeNode.getReference());
            Assert.assertEquals(1, treeNode.getChildren().size());

            treeNode = treeNode.get("space2");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new SpaceReference("space2", new WikiReference("wiki2")), treeNode.getReference());
            Assert.assertEquals(1, treeNode.getChildren().size());

            treeNode = treeNode.get("page2");

            Assert.assertNotNull(treeNode);
            Assert.assertEquals(new DocumentReference("wiki2", "space2", "page2"), treeNode.getReference());
            Assert.assertEquals(0, treeNode.getChildren().size());
        }
    }

    @Test
    public void getDescendantByReference()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        EntityReferenceTree tree = new EntityReferenceTree(documentReference);

        Assert.assertSame(documentReference.getWikiReference(), tree.get(new WikiReference("wiki")).getReference());

        Assert.assertSame(documentReference.getLastSpaceReference(),
            tree.get(documentReference.getLastSpaceReference()).getReference());

        EntityReference entityReference =
            new EntityReference(documentReference.getName(), EntityType.DOCUMENT, documentReference.getParent());
        Assert.assertSame(documentReference, tree.get(entityReference).getReference());

        // The entity type should be taken into account.
        Assert.assertNull(tree
            .get(new EntityReference(documentReference.getName(), EntityType.SPACE, documentReference.getParent())));

        Assert.assertNull(tree.get((EntityReference) null));

        Assert.assertNull(tree.get(new SpaceReference("From", documentReference.getParent().getParent())));
    }

    @Test
    public void testSiblingWithDifferentType()
    {
        DocumentReference document = new DocumentReference("wiki", "space", "entity");
        SpaceReference space = new SpaceReference("wiki", "space", "entity");

        EntityReferenceTreeNode tree = new EntityReferenceTree(document, space);

        Assert.assertSame(document, tree.get(document).getReference());
        Assert.assertSame(space, tree.get(space).getReference());

        EntityReferenceTreeNode treeNode = tree;

        Assert.assertEquals(null, treeNode.getReference());
        Assert.assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("wiki");

        Assert.assertNotNull(treeNode);
        Assert.assertSame(document.getWikiReference(), treeNode.getReference());
        Assert.assertEquals(1, treeNode.getChildren().size());

        treeNode = treeNode.get("space");

        Assert.assertNotNull(treeNode);
        Assert.assertSame(document.getParent(), treeNode.getReference());
        Assert.assertEquals(2, treeNode.getChildren().size());
        Assert.assertEquals(1, treeNode.getChildren(EntityType.DOCUMENT).size());
        Assert.assertEquals(1, treeNode.getChildren(EntityType.SPACE).size());

        EntityReferenceTreeNode documentNode = treeNode.getChildren(EntityType.DOCUMENT).iterator().next();

        Assert.assertNotNull(documentNode);
        Assert.assertSame(document, documentNode.getReference());
        Assert.assertEquals(0, documentNode.getChildren().size());

        EntityReferenceTreeNode spaceNode = treeNode.getChildren(EntityType.SPACE).iterator().next();

        Assert.assertNotNull(spaceNode);
        Assert.assertSame(space, spaceNode.getReference());
        Assert.assertEquals(0, spaceNode.getChildren().size());
    }
}
