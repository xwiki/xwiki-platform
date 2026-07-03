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
package org.xwiki.flamingo.test.docker;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.skin.test.po.ExportTreeModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify that the XAR export features works fine.
 *
 * @version $Id$
 * @since 10.9
 */
@UITest
public class XARExportIT
{
    @BeforeEach
    void setupPages(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        // Create a space Foo
        testUtils.createPage("Foo", "WebHome", "Foo", "Foo");

        // Create 50 pages under that space (will be used for the test with lot of selected pages)
        for (int i = 0; i < 50; i++) {
            String name = "Foo_" + i;
            DocumentReference documentReference = new DocumentReference("xwiki", List.of("Foo", name), "WebHome");
            if (!testUtils.rest().exists(documentReference)) {
                testUtils.rest().savePage(documentReference);
            }
        }

        for (int i = 0; i < 20; i++) {
            String name = "Foo_10_" + i;
            DocumentReference documentReference =
                new DocumentReference("xwiki", List.of("Foo", "Foo_10", name), "WebHome");
            if (!testUtils.rest().exists(documentReference)) {
                testUtils.rest().savePage(documentReference);
            }
        }
    }

    /**
     * Scenario: export a XAR after opening the export window and selecting "Other Format". Don't change anything in the
     * tree of export.
     */
    @Test
    @Order(1)
    void exportXARAll(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        ViewPage viewPage = testUtils.gotoPage("Foo", "WebHome");
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "XAR");

        TreeElement treeElement = exportTreeModal.getPageTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        exportTreeModal.export();

        String postURL = testUtils.getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome&");
        assertEquals(postURL, exportTreeModal.getAction());

        assertEquals(List.of("xwiki:Foo.%"), exportTreeModal.getPagesValues());
        assertEquals(List.of(""), exportTreeModal.getExcludesValues());
        testUtils.forceGuestUser();
    }

    /**
     * Scenario: test with lots of selected pages in the XAR expor and spanning several pages to ensure that everything
     * selected is taken into account (See XWIKI-15444). To achieve this, we open every page in the pagination and
     * select everything.
     */
    @Test
    @Order(2)
    void exportXARLotOfSelectedFiles(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();

        ViewPage viewPage = testUtils.gotoPage("Foo", "WebHome");
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "XAR");

        TreeElement treeElement = exportTreeModal.getPageTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement root = topLevelNodes.get(0);
        root.open().waitForIt();

        assertEquals(16, root.getChildren().size());

        TreeNodeElement lastNode;
        int size = 0;
        for (int i = 15; i < 50; i += 15) {
            size = root.getChildren().size();
            lastNode = root.getChildren().get(size - 1);

            String lastNodeLabel = lastNode.getLabel();
            lastNode.deselect();
            lastNode.select();

            testUtils.getDriver().waitUntilElementDisappears(exportTreeModal.getContainer(),
                By.linkText(lastNodeLabel));
        }

        assertEquals(50, root.getChildren().size());

        exportTreeModal.export();

        String postURL = testUtils.getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome&");
        assertEquals(postURL, exportTreeModal.getAction());

        assertEquals(List.of("xwiki:Foo.%"), exportTreeModal.getPagesValues());
        assertEquals(List.of(""), exportTreeModal.getExcludesValues());

        testUtils.forceGuestUser();
    }

    /**
     * Scenario: Export a XAR after opening main tree and a subtree and unselecting nodes including the pagination node
     * of the subtree.
     */
    @Test
    @Order(3)
    public void exportXARWithUnselect(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        ViewPage viewPage = testUtils.gotoPage("Foo", "WebHome");
        ExportTreeModal exportTreeModal = ExportTreeModal.open(viewPage, "XAR");

        TreeElement treeElement = exportTreeModal.getPageTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement root = topLevelNodes.get(0);
        root = root.open().waitForIt();

        assertEquals(16, root.getChildren().size());

        TreeNodeElement node2 = root.getChildren().get(2);
        TreeNodeElement node4 = root.getChildren().get(4);
        TreeNodeElement node6 = root.getChildren().get(6);

        // nodes are ordered using alphabetical order:
        // 1 -> Foo_1
        // 2 -> Foo_10
        // ...
        // 4 -> Foo_12
        // 6 -> Foo_14

        node4.deselect();
        node6.deselect();
        node2.open().waitForIt();

        // the DOM is rebuilt
        node2 = root.getChildren().get(2);

        assertEquals(16, node2.getChildren().size());
        node2.deselect();

        // 1 -> Foo_10/Foo_10_1
        node2.getChildren().get(1).select();

        // 2 -> Foo_10/Foo_10_15
        node2.getChildren().get(7).select();

        exportTreeModal.export();

        String postURL = testUtils.getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome&");
        assertEquals(postURL, exportTreeModal.getAction());

        List<String> expectedPages = new ArrayList<>();
        expectedPages.add("xwiki:Foo.%");
        expectedPages.add("xwiki:Foo.Foo_10.Foo_10_1.WebHome");
        expectedPages.add("xwiki:Foo.Foo_10.Foo_10_15.WebHome");

        List<String> expectedExcludes = new ArrayList<>();
        expectedExcludes.add("xwiki%3AFoo.Foo_10.%25&xwiki%3AFoo.Foo_12.WebHome&xwiki%3AFoo.Foo_14.WebHome");
        // we must keep the same number of argument as for pages
        expectedExcludes.add("");
        expectedExcludes.add("");

        assertEquals(expectedPages, exportTreeModal.getPagesValues());
        assertEquals(expectedExcludes, exportTreeModal.getExcludesValues());

        testUtils.forceGuestUser();
    }
}
