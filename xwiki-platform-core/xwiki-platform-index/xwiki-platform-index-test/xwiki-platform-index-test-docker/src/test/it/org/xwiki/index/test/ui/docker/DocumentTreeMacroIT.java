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
package org.xwiki.index.test.ui.docker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Document Tree Macro.
 *
 * @version $Id$
 * @since 16.10.3
 * @since 17.0.0RC1
 */
@ExtendWith(DynamicTestConfigurationExtension.class)
@UITest
class DocumentTreeMacroIT
{
    @Test
    @Order(1)
    void sortDocumentsBy(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference alice =
            new DocumentReference("WebHome", new SpaceReference("Alice", testReference.getLastSpaceReference()));
        DocumentReference bob =
            new DocumentReference("WebHome", new SpaceReference("Bob", testReference.getLastSpaceReference()));
        DocumentReference carolSecond = new DocumentReference("WebHome",
            new SpaceReference("Second", new SpaceReference("Carol", testReference.getLastSpaceReference())));
        DocumentReference george =
            new DocumentReference("WebHome", new SpaceReference("George", testReference.getLastSpaceReference()));

        DocumentReference denis = new DocumentReference("Denis", alice.getLastSpaceReference());
        DocumentReference eve =
            new DocumentReference("WebHome", new SpaceReference("Eve", alice.getLastSpaceReference()));
        DocumentReference fionaThird = new DocumentReference("WebHome",
            new SpaceReference("Third", new SpaceReference("Fiona", alice.getLastSpaceReference())));
        DocumentReference henry = new DocumentReference("Henry", alice.getLastSpaceReference());

        setup.loginAsSuperAdmin();

        // Clean up.
        setup.deletePage(testReference, true);

        // Setup.
        createPage(setup, carolSecond, "Second", "");
        // Make sure the creation date is different for each document (taking into account that milliseconds are lost
        // when the date is saved in the database), otherwise we can't verify the sort on creation date.
        Thread.sleep(1000);
        createPage(setup, george, "2. George", "");
        Thread.sleep(1000);
        createPage(setup, alice, "3. Alice", "");
        Thread.sleep(1000);
        createPage(setup, bob, "1. Bob", "");

        Thread.sleep(1000);
        createPage(setup, eve, "3. Eve", "");
        Thread.sleep(1000);
        createPage(setup, henry, "1. Henry", "");
        Thread.sleep(1000);
        createPage(setup, fionaThird, "Third", "");
        Thread.sleep(1000);
        createPage(setup, denis, "2. Denis", "");

        // Make sure the last modification date is different for each document (taking into account that milliseconds
        // are lost when the date is saved in the database), otherwise we can't verify the sort on modification date.
        Thread.sleep(1000);
        setup.rest().savePage(alice, "updated", "3. Alice");
        Thread.sleep(1000);
        setup.rest().savePage(eve, "updated", "3. Eve");

        // Documents are sorted by title by default (when document titles are displayed).
        TreeElement tree = getDocumentTree(setup, testReference, Map.of("showTerminalDocuments", false));
        assertNodeLabels(tree.getTopLevelNodes(), "1. Bob", "2. George", "3. Alice", "Carol");

        // Documents are sorted by name when document titles are not displayed.
        tree =
            getDocumentTree(setup, testReference, Map.of("showTerminalDocuments", false, "showDocumentTitle", false));
        assertNodeLabels(tree.getTopLevelNodes(), "Alice", "Bob", "Carol", "George");

        // Sort by title descending.
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "title:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "Carol", "3. Alice", "2. George", "1. Bob");

        // Sort by name descending.
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "name:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "2. George", "Carol", "1. Bob", "3. Alice");

        // Sort by last modification date descending (null values last).
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "date:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "3. Alice", "1. Bob", "2. George", "Carol");

        // Sort by creation date ascending (null values last).
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "creationDate"));
        assertNodeLabels(tree.getTopLevelNodes(), "2. George", "3. Alice", "1. Bob", "Carol");

        // Let's check also the order of descendant pages (where we mix terminal and non-terminal pages).

        // Documents are sorted by title by default (when document titles are displayed).
        tree = getDocumentTree(setup, testReference, Map.of("openTo", getNodeId(alice)));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "1. Henry", "2. Denis", "3. Eve", "Fiona");

        // Sort by name (even if document titles are displayed).
        tree = getDocumentTree(setup, testReference, Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "name"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "2. Denis", "3. Eve", "Fiona", "1. Henry");

        // Sort by title descending.
        tree =
            getDocumentTree(setup, testReference, Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "title:desc"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "Fiona", "3. Eve", "2. Denis", "1. Henry");

        // Sort by last modification date ascending (null values last).
        tree = getDocumentTree(setup, testReference, Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "date:asc"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "1. Henry", "2. Denis", "3. Eve", "Fiona");

        // Sort by creation date descending (null values last).
        tree = getDocumentTree(setup, testReference,
            Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "creationDate:desc"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "2. Denis", "1. Henry", "3. Eve", "Fiona");

        // Verify the sort on the Children macro, which is a simple wrapper around the Document Tree macro.

        // Child documents are sorted by title by default (when document titles are displayed).
        tree = getChildrenTree(setup, alice, Map.of());
        assertNodeLabels(tree.getTopLevelNodes(), "1. Henry", "2. Denis", "3. Eve", "Fiona");

        // Sort by name (even if document titles are displayed).
        tree = getChildrenTree(setup, alice, Map.of("sort", "name"));
        assertNodeLabels(tree.getTopLevelNodes(), "2. Denis", "3. Eve", "Fiona", "1. Henry");

        // Sort by title descending.
        tree = getChildrenTree(setup, alice, Map.of("sort", "title:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "Fiona", "3. Eve", "2. Denis", "1. Henry");

        // Sort by last modification date descending (null values last).
        tree = getChildrenTree(setup, alice, Map.of("sort", "date:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "3. Eve", "2. Denis", "1. Henry", "Fiona");
    }

    @Test
    @Order(2)
    void expandToLevel(TestUtils setup, TestReference testReference)
    {
        SpaceReference testSpaceReference = testReference.getLastSpaceReference();

        // Create the following hierarchy:
        // TestReference:
        //    - Alice:
        //        - SubAlice
        //    - Bob
        //    - Carol (terminal)
        //    - Eve:
        //        - SubEve
        //           - SubEveChild
        //        - SubEve2
        //    - Fiona
        //        - FionaTerminal

        DocumentReference alice =
            new DocumentReference("WebHome", new SpaceReference("Alice", testSpaceReference));
        DocumentReference subAlice =
            new DocumentReference("WebHome", new SpaceReference("SubAlice", alice.getLastSpaceReference()));

        DocumentReference bob =
            new DocumentReference("WebHome", new SpaceReference("Bob", testSpaceReference));
        DocumentReference carol = new DocumentReference("Carol", testSpaceReference);

        DocumentReference eve =
            new DocumentReference("WebHome", new SpaceReference("Eve", testSpaceReference));
        DocumentReference subEve =
            new DocumentReference("WebHome", new SpaceReference("SubEve", eve.getLastSpaceReference()));
        DocumentReference subEveChild =
            new DocumentReference("WebHome", new SpaceReference("SubEveChild", subEve.getLastSpaceReference()));
        DocumentReference subEve2 =
            new DocumentReference("WebHome", new SpaceReference("SubEve2", eve.getLastSpaceReference()));

        DocumentReference fiona =
            new DocumentReference("WebHome", new SpaceReference("Fiona", testSpaceReference));
        DocumentReference fionaTerminal =
            new DocumentReference("FionaTerminal", fiona.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        // Clean up.
        setup.deletePage(testReference, true);
        createPage(setup, alice, "", "");
        createPage(setup, subAlice, "", "");
        createPage(setup, bob, "", "");
        createPage(setup, carol, "", "");
        createPage(setup, eve, "", "");
        createPage(setup, subEve, "", "");
        createPage(setup, subEveChild, "", "");
        createPage(setup, subEve2, "", "");
        createPage(setup, fiona, "", "");
        createPage(setup, fionaTerminal, "", "");

        // By default only top nodes are displayed
        TreeElement tree = getDocumentTree(setup, testReference, Map.of("root", getNodeId(testReference)));
        assertNodeLabels(tree.getTopLevelNodes(), "Alice", "Bob", "Carol", "Eve", "Fiona");
        tree.getTopLevelNodes().forEach(node -> assertFalse(node.isOpen()));

        // Specifying expandToLevel 0 should be exactly the same
        tree = getDocumentTree(setup, testReference, Map.of("root", getNodeId(testReference), "expandToLevel", "0"));
        assertNodeLabels(tree.getTopLevelNodes(), "Alice", "Bob", "Carol", "Eve", "Fiona");
        tree.getTopLevelNodes().forEach(node -> assertFalse(node.isOpen()));

        // Now all top nodes are opened
        tree = getDocumentTree(setup, testReference, Map.of("root", getNodeId(testReference), "expandToLevel", "1"));
        List<TreeNodeElement> topLevelNodes = tree.getTopLevelNodes();
        assertNodeLabels(topLevelNodes, "Alice", "Bob", "Carol", "Eve", "Fiona");

        // Alice
        assertTrue(topLevelNodes.get(0).isOpen());
        assertNodeLabels(topLevelNodes.get(0).getChildren(), "SubAlice");

        // Bob and Carol cannot be opened
        assertFalse(topLevelNodes.get(1).isOpen());
        assertFalse(topLevelNodes.get(2).isOpen());

        // Eve
        assertTrue(topLevelNodes.get(3).isOpen());
        assertNodeLabels(topLevelNodes.get(3).getChildren(), "SubEve", "SubEve2");
        // Ensure only 1 level is open
        assertFalse(topLevelNodes.get(3).getChildren().get(0).isOpen());

        // Fiona
        assertTrue(topLevelNodes.get(4).isOpen());
        assertNodeLabels(topLevelNodes.get(4).getChildren(), "FionaTerminal");

        // Check that expandToLevel=2 also open sublevel
        tree = getDocumentTree(setup, testReference, Map.of("root", getNodeId(testReference), "expandToLevel", "2"));
        topLevelNodes = tree.getTopLevelNodes();
        assertNodeLabels(topLevelNodes, "Alice", "Bob", "Carol", "Eve", "Fiona");

        // Alice
        assertTrue(topLevelNodes.get(0).isOpen());
        assertNodeLabels(topLevelNodes.get(0).getChildren(), "SubAlice");
        // No children so cannot be open
        assertFalse(topLevelNodes.get(0).getChildren().get(0).isOpen());

        // Bob and Carol cannot be opened
        assertFalse(topLevelNodes.get(1).isOpen());
        assertFalse(topLevelNodes.get(2).isOpen());

        // Eve
        assertTrue(topLevelNodes.get(3).isOpen());
        assertNodeLabels(topLevelNodes.get(3).getChildren(), "SubEve", "SubEve2");
        assertTrue(topLevelNodes.get(3).getChildren().get(0).isOpen());
        assertNodeLabels(topLevelNodes.get(3).getChildren().get(0).getChildren(), "SubEveChild");

        // Fiona
        assertTrue(topLevelNodes.get(4).isOpen());
        assertNodeLabels(topLevelNodes.get(4).getChildren(), "FionaTerminal");
    }

    @Test
    @Order(3)
    void childrenRoot(TestUtils setup, TestReference testReference)
    {
        SpaceReference testSpaceReference = testReference.getLastSpaceReference();

        // Create the following hierarchy:
        // TestReference:
        //    - Alice:
        //        - SubAlice
        //    - Eve:
        //        - SubEve
        //        - SubEve2

        DocumentReference alice =
            new DocumentReference("WebHome", new SpaceReference("Alice", testSpaceReference));
        DocumentReference subAlice =
            new DocumentReference("WebHome", new SpaceReference("SubAlice", alice.getLastSpaceReference()));

        DocumentReference eve =
            new DocumentReference("WebHome", new SpaceReference("Eve", testSpaceReference));
        DocumentReference subEve =
            new DocumentReference("WebHome", new SpaceReference("SubEve", eve.getLastSpaceReference()));
        DocumentReference subEve2 =
            new DocumentReference("WebHome", new SpaceReference("SubEve2", eve.getLastSpaceReference()));

        setup.loginAsSuperAdmin();
        // Clean up.
        setup.deletePage(testReference, true);
        createPage(setup, alice, "", "");
        createPage(setup, subAlice, "", "");
        createPage(setup, eve, "", "");
        createPage(setup, subEve, "", "");
        createPage(setup, subEve2, "", "");

        // By default only top nodes of current page are displayed
        TreeElement tree = getChildrenTree(setup, testReference, Map.of());
        assertNodeLabels(tree.getTopLevelNodes(), "Alice", "Eve");
        tree.getTopLevelNodes().forEach(node -> assertFalse(node.isOpen()));

        // Set root to Alice displays only Alice nodes
        tree = getChildrenTree(setup, testReference, Map.of("root", getNodeId(alice)));
        assertNodeLabels(tree.getTopLevelNodes(), "SubAlice");

        // Set root to Eve displays only Eve nodes
        tree = getChildrenTree(setup, testReference, Map.of("root", getNodeId(eve)));
        assertNodeLabels(tree.getTopLevelNodes(), "SubEve", "SubEve2");
    }

    @Test
    @Order(4)
    void sortWithCollation(TestUtils setup, TestReference testReference)
    {
        DocumentReference alice =
            new DocumentReference("WebHome", new SpaceReference("Alice", testReference.getLastSpaceReference()));
        DocumentReference bob =
            new DocumentReference("WebHome", new SpaceReference("Öl", testReference.getLastSpaceReference()));
        DocumentReference george =
            new DocumentReference("WebHome", new SpaceReference("Zeit", testReference.getLastSpaceReference()));

        setup.loginAsSuperAdmin();
        setup.deletePage(testReference, true);

        try {
            createPage(setup, alice, "Ö Alice", "");
            createPage(setup, bob, "A Öl", "");
            createPage(setup, george, "Zeit", "");

            // Sort by title with collation
            TreeElement tree = getDocumentTree(setup, testReference, Map.of());
            assertNodeLabels(tree.getTopLevelNodes(), "A Öl", "Ö Alice", "Zeit");

            // Sort by name with collation (even if document titles are displayed).
            tree = getChildrenTree(setup, testReference, Map.of("sort", "name"));
            assertNodeLabels(tree.getTopLevelNodes(), "Ö Alice", "A Öl", "Zeit");
        } finally {
            // Cleanup.
            setup.deletePage(testReference, true);
        }
    }

    private ViewPage createPage(TestUtils setup, DocumentReference documentReference, String title, String content)
    {
        // We don't care what parent page is used, we just want to avoid creating orphan pages in order to not interfere
        // with other tests in this module.
        return setup.createPage(documentReference, content, title, "xwiki/2.1", "Main.WebHome");
    }

    private TreeElement getDocumentTree(TestUtils setup, TestReference testReference, Map<String, Object> parameters)
    {
        parameters = new HashMap<>(parameters);
        String id = testReference.getLastSpaceReference().getName();
        parameters.put("id", id);
        parameters.put("root", getNodeId(testReference));
        parameters.put("showRoot", false);
        StringBuilder content = new StringBuilder("{{documentTree");
        parameters.forEach((key, value) ->
            content.append(" ").append(key).append("='").append(value).append("'"));
        content.append("/}}");
        createPage(setup, testReference, "", content.toString());
        return new TreeElement(setup.getDriver().findElement(By.id(id))).waitForIt();
    }

    private TreeElement getChildrenTree(TestUtils setup, DocumentReference parentReference,
        Map<String, Object> parameters)
    {
        StringBuilder content = new StringBuilder("{{children");
        parameters.forEach((key, value) ->
            content.append(" ").append(key).append("='").append(value).append("'"));
        content.append("/}}");
        createPage(setup, parentReference, "", content.toString());
        return new TreeElement(setup.getDriver().findElement(By.cssSelector("#xwikicontent .xtree"))).waitForIt();
    }

    private String getNodeId(DocumentReference documentReference)
    {
        return "document:" + documentReference.toString();
    }

    private void assertNodeLabels(List<TreeNodeElement> nodes, String... labels)
    {
        List<String> expectedLabels = List.of(labels);
        List<String> actualLabels = nodes.stream().map(TreeNodeElement::getLabel).toList();
        assertEquals(expectedLabels, actualLabels);
    }
}
