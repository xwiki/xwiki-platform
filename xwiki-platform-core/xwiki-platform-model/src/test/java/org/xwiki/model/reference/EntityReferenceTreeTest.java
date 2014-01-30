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

import org.junit.Assert;
import org.junit.Test;

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
        EntityReferenceTreeNode tree =
            new EntityReferenceTree(new DocumentReference("wiki", "space", "page"), new DocumentReference("wiki",
                "space2", "page2"), new DocumentReference("wiki", "space", "page2"), new DocumentReference("wiki2",
                "space2", "page2"));

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
}
