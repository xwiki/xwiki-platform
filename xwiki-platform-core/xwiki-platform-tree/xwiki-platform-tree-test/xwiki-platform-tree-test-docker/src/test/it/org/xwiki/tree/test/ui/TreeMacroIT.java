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
package org.xwiki.tree.test.ui;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Tree macro.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@UITest
class TreeMacroIT
{
    @Test
    void displayTreeWithNodesDefinedInWikiSyntax(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, """
            {{tree}}
            * [[Chapter 1>>Main.WebHome]]
            ** Section 1.1
            ** [[Section 1.2>>Sandbox.WebHome]]
            * Chapter 2
            {{/tree}}""", "Tree Macro");

        // The links used to define the tree nodes are un-wrapped, so that jsTree takes them into account, but only the
        // tree element itself must be turned into a tree.
        List<TreeElement> trees = TreeElement.getTreesInPageContent();
        assertEquals(1, trees.size());

        TreeElement tree = trees.get(0).waitForIt();

        List<TreeNodeElement> topLevelNodes = tree.getTopLevelNodes();
        assertEquals(List.of("Chapter 1", "Chapter 2"), topLevelNodes.stream().map(TreeNodeElement::getLabel).toList());

        List<TreeNodeElement> children = topLevelNodes.get(0).open().waitForIt().getChildren();
        assertEquals(List.of("Section 1.1", "Section 1.2"), children.stream().map(TreeNodeElement::getLabel).toList());
    }

    @Test
    void displayTwoTreesWithTheSameNodes(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        // Both trees define the same nodes, with the same node ids, so that we can verify that the rendered ids are
        // still unique on the page.
        String treeMarkup = """
            {{tree}}
            {{html}}
            <ul>
              <li id="chapter1" class="jstree-open"><a href="https://www.xwiki.org">Chapter 1</a>
                <ul>
                  <li id="section11">Section 1.1</li>
                </ul>
              </li>
              <li id="chapter2">Chapter 2</li>
            </ul>
            {{/html}}
            {{/tree}}""";
        setup.createPage(testReference, treeMarkup + "\n\n" + treeMarkup, "Two Trees");

        List<TreeElement> trees = TreeElement.getTreesInPageContent();
        assertEquals(2, trees.size());
        trees.forEach(TreeElement::waitForIt);

        // Both trees display the same nodes.
        for (TreeElement tree : trees) {
            assertEquals(List.of("Chapter 1", "Chapter 2"),
                tree.getTopLevelNodes().stream().map(TreeNodeElement::getLabel).toList());
        }

        // The node labels of both trees must have an id that is unique on the page, even though the two trees display
        // the same nodes (the node id alone is not enough, so it is prefixed with the tree id).
        List<String> nodeLabelIds = trees.stream().flatMap(tree -> tree.getNodeLabelIds().stream()).toList();
        assertEquals(6, nodeLabelIds.size());
        assertTrue(nodeLabelIds.stream().allMatch(id -> id != null && !id.isEmpty()),
            () -> "Some node labels have no id: " + nodeLabelIds);
        assertEquals(nodeLabelIds.size(), Set.copyOf(nodeLabelIds).size(),
            () -> "Duplicate node label ids: " + nodeLabelIds);
    }
}
