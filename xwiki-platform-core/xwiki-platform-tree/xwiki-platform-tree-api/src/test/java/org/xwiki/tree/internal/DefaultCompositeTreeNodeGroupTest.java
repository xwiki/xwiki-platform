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
package org.xwiki.tree.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.tree.CompositeTreeNodeGroup;
import org.xwiki.tree.TreeNode;
import org.xwiki.tree.TreeNodeGroup;

/**
 * Unit tests for {@link DefaultCompositeTreeNodeGroup}.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@ComponentTest
class DefaultCompositeTreeNodeGroupTest
{
    @InjectMockComponents(role = CompositeTreeNodeGroup.class)
    private DefaultCompositeTreeNodeGroup compositeTreeNodeGroup;

    @Test
    void getChildren()
    {
        TreeNode alice = mock(TreeNode.class, "alice");
        this.compositeTreeNodeGroup.addTreeNode(alice, nodeId -> nodeId.endsWith("/"));

        TreeNode bob = mock(TreeNode.class, "bob");
        this.compositeTreeNodeGroup.addTreeNode(bob, nodeId -> nodeId.startsWith("/"));

        TreeNodeGroup carol = mock(TreeNodeGroup.class, "carol");
        this.compositeTreeNodeGroup.addTreeNode(carol, nodeId -> nodeId.startsWith("/") && nodeId.endsWith("/"));

        TreeNodeGroup denis = mock(TreeNodeGroup.class, "denis");
        this.compositeTreeNodeGroup.addTreeNode(denis, nodeId -> true);

        TreeNode eve = mock(TreeNode.class, "eve");
        this.compositeTreeNodeGroup.addTreeNode(eve, nodeId -> nodeId.contains(":"));

        when(carol.getChildCount("/te:st/")).thenReturn(3);
        when(denis.getChildCount("/te:st/")).thenReturn(2);
        when(denis.getChildCount("/te:st")).thenReturn(1);
        when(denis.getChildCount("te:st")).thenReturn(0);
        when(denis.getChildCount("test")).thenReturn(3);

        assertEquals(0, this.compositeTreeNodeGroup.getChildCount("foo"));
        assertEquals(8, this.compositeTreeNodeGroup.getChildCount("/te:st/"));
        assertEquals(3, this.compositeTreeNodeGroup.getChildCount("/te:st"));
        assertEquals(1, this.compositeTreeNodeGroup.getChildCount("te:st"));
        assertEquals(3, this.compositeTreeNodeGroup.getChildCount("test"));

        assertEquals(List.of(), this.compositeTreeNodeGroup.getChildren("foo", 0, 10));
        assertEquals(List.of(), this.compositeTreeNodeGroup.getChildren("/te:st/", 8, 3));
        assertEquals(List.of(), this.compositeTreeNodeGroup.getChildren("/te:st", 5, -1));

        this.compositeTreeNodeGroup
            .setIdGenerator((parentId, childNodeType) -> String.format("%s:%s", childNodeType, parentId));
        when(alice.getType()).thenReturn("alice");
        when(bob.getType()).thenReturn("bob");
        when(eve.getType()).thenReturn("eve");

        when(carol.getChildren("/te:st/", 0, -1)).thenReturn(List.of("carol_1", "carol_2", "carol_3"));
        when(denis.getChildren("/te:st/", 0, -1)).thenReturn(List.of("denis_1", "denis_2"));
        assertEquals(List.of("alice:/te:st/", "bob:/te:st/", "carol_1", "carol_2", "carol_3", "denis_1", "denis_2",
            "eve:/te:st/"), this.compositeTreeNodeGroup.getChildren("/te:st/", 0, -1));

        when(carol.getChildren("/te:st/", 0, 5)).thenReturn(List.of("carol_1", "carol_2", "carol_3"));
        when(denis.getChildren("/te:st/", 0, 2)).thenReturn(List.of("denis_1", "denis_2"));
        assertEquals(List.of("bob:/te:st/", "carol_1", "carol_2", "carol_3", "denis_1", "denis_2"),
            this.compositeTreeNodeGroup.getChildren("/te:st/", 1, 6));

        when(carol.getChildren("/te:st/", 1, 3)).thenReturn(List.of("carol_2", "carol_3"));
        when(denis.getChildren("/te:st/", 0, 1)).thenReturn(List.of("denis_1"));
        assertEquals(List.of("carol_2", "carol_3", "denis_1"),
            this.compositeTreeNodeGroup.getChildren("/te:st/", 3, 3));

        when(denis.getChildren("/te:st/", 1, 1)).thenReturn(List.of("denis_2"));
        assertEquals(List.of("denis_2"), this.compositeTreeNodeGroup.getChildren("/te:st/", 6, 1));
        verify(carol, times(3)).getChildren(anyString(), anyInt(), anyInt());

        assertEquals(List.of("eve:/te:st/"), this.compositeTreeNodeGroup.getChildren("/te:st/", 7, 10));
        verify(carol, times(3)).getChildren(anyString(), anyInt(), anyInt());
        verify(denis, times(4)).getChildren(anyString(), anyInt(), anyInt());
    }
}
