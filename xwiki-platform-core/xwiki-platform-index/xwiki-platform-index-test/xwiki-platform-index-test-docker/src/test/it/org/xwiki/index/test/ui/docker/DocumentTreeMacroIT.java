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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Functional tests for the Document Tree Macro.
 *
 * @version $Id$
 * @since 16.10.3
 * @since 17.0.0RC1
 */
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
        createPage(setup, carolSecond, "Second");
        // Make sure the creation date is different for each document (taking into account that milliseconds are lost
        // when the date is saved in the database), otherwise we can't verify the sort on creation date.
        Thread.sleep(1000);
        createPage(setup, george, "2. George");
        Thread.sleep(1000);
        createPage(setup, alice, "3. Alice");
        Thread.sleep(1000);
        createPage(setup, bob, "1. Bob");

        Thread.sleep(1000);
        createPage(setup, eve, "3. Eve");
        Thread.sleep(1000);
        createPage(setup, henry, "1. Henry");
        Thread.sleep(1000);
        createPage(setup, fionaThird, "Third");
        Thread.sleep(1000);
        createPage(setup, denis, "2. Denis");

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

        // Sort by last modification date descending.
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "date:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "Carol", "3. Alice", "1. Bob", "2. George");

        // Sort by creation date ascending.
        tree = getDocumentTree(setup, testReference,
            Map.of("showTerminalDocuments", false, "sortDocumentsBy", "creationDate"));
        assertNodeLabels(tree.getTopLevelNodes(), "Carol", "2. George", "3. Alice", "1. Bob");

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

        // Sort by last modification date ascending.
        tree = getDocumentTree(setup, testReference, Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "date:asc"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "Fiona", "1. Henry", "2. Denis", "3. Eve");

        // Sort by creation date descending.
        tree = getDocumentTree(setup, testReference,
            Map.of("openTo", getNodeId(alice), "sortDocumentsBy", "creationDate:desc"));
        assertNodeLabels(tree.getNode(getNodeId(alice)).getChildren(), "Fiona", "2. Denis", "1. Henry", "3. Eve");

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

        // Sort by last modification date descending.
        tree = getChildrenTree(setup, alice, Map.of("sort", "date:desc"));
        assertNodeLabels(tree.getTopLevelNodes(), "Fiona", "3. Eve", "2. Denis", "1. Henry");
    }

    private ViewPage createPage(TestUtils setup, DocumentReference documentReference, String title)
    {
        // We don't care what parent page is used, we just want to avoid creating orphan pages in order to not interfere
        // with other tests in this module.
        return setup.createPage(documentReference, "", title, "xwiki/2.1", "Main.WebHome");
    }

    private TreeElement getDocumentTree(TestUtils setup, TestReference testReference, Map<String, Object> parameters)
    {
        parameters = new HashMap<>(parameters);
        String id = testReference.getLastSpaceReference().getName();
        parameters.put("id", id);
        parameters.put("root", getNodeId(testReference));
        parameters.put("showRoot", false);
        StringBuilder content = new StringBuilder("{{documentTree");
        parameters.forEach((key, value) -> content.append(" ").append(key).append("='").append(value).append("'"));
        content.append("/}}");
        setup.createPage(testReference, content.toString(), "", "xwiki/2.1", "Main.WebHome");
        return new TreeElement(setup.getDriver().findElement(By.id(id))).waitForIt();
    }

    private TreeElement getChildrenTree(TestUtils setup, DocumentReference parentReference,
        Map<String, Object> parameters)
    {
        StringBuilder content = new StringBuilder("{{children");
        parameters.forEach((key, value) -> content.append(" ").append(key).append("='").append(value).append("'"));
        content.append("/}}");
        setup.createPage(parentReference, content.toString(), "", "xwiki/2.1", "Main.WebHome");
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
