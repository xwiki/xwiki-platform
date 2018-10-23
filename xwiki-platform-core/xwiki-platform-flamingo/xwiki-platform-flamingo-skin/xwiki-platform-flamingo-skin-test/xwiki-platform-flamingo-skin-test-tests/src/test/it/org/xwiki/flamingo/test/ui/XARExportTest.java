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
package org.xwiki.flamingo.test.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.flamingo.sking.test.po.ExportModal;
import org.xwiki.flamingo.sking.test.po.OtherFormatPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verify that the XAR export features works fine.
 *
 * @version $Id$
 * @since 10.9RC1
 */
public class XARExportTest extends AbstractTest
{
    @Test
    public void scenarioExportXAR() throws Exception
    {
        setupPages();
        exportXARAll();
        exportXARLotOfSelectedFiles();
        exportXARWithUnselect();
    }

    private void setupPages() throws Exception
    {
        getUtil().loginAsSuperAdmin();

        // Create a space Foo
        getUtil().createPage("Foo", "WebHome", "Foo", "Foo");

        // Create 50 pages under that space (will be used for the test with lot of selected pages)
        for (int i = 0; i < 50; i++) {
            String name = "Foo_" + i;
            DocumentReference documentReference = new DocumentReference("xwiki",
                Arrays.asList("Foo", name), "WebHome");
            if (!getUtil().rest().exists(documentReference)) {
                getUtil().rest().savePage(documentReference);
            }
        }

        for (int i = 0; i < 20; i++) {
            String name = "Foo_10_"+i;
            DocumentReference documentReference = new DocumentReference("xwiki",
                Arrays.asList("Foo", "Foo_10", name), "WebHome");
            if (!getUtil().rest().exists(documentReference)) {
                getUtil().rest().savePage(documentReference);
            }
        }
        // change the timeout as it might take time to load all nodes
        getDriver().setTimeout(20);
    }

    /*
       Scenario: export a XAR after opening the export window and selecting "Other Format"
       Don't change anything in the tree of export.
     */
    private void exportXARAll() throws Exception
    {
        getUtil().loginAsSuperAdmin();
        ViewPage viewPage = getUtil().gotoPage("Foo", "WebHome");
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        ExportModal exportModal = new ExportModal();

        OtherFormatPane otherFormatPane = exportModal.openOtherFormatPane();
        assertTrue(otherFormatPane.isTreeAvailable());
        assertTrue(otherFormatPane.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatPane.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        otherFormatPane.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome",
            "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatPane.getForm().getAttribute("action"));

        List<String> expectedPages = new ArrayList<>();
        expectedPages.add("xwiki:Foo.%");
        assertEquals(expectedPages, otherFormatPane.getPagesValues());

        List<String> expectedExcludes = new ArrayList<>();
        expectedExcludes.add("");
        assertEquals(expectedExcludes, otherFormatPane.getExcludesValues());
        getUtil().forceGuestUser();
    }

    /*
        Scenario: Export a XAR after opening every pages in the pagination
        and selecting everything
     */
    public void exportXARLotOfSelectedFiles()
    {
        getUtil().loginAsSuperAdmin();
        ViewPage viewPage = getUtil().gotoPage("Foo", "WebHome");
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        ExportModal exportModal = new ExportModal();
        OtherFormatPane otherFormatPane = exportModal.openOtherFormatPane();
        assertTrue(otherFormatPane.isTreeAvailable());
        assertTrue(otherFormatPane.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatPane.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement root = topLevelNodes.get(0);
        root.open().waitForIt();

        assertEquals(16, root.getChildren().size());

        TreeNodeElement lastNode = null;
        int size = 0;
        for (int i = 15; i < 50; i += 15) {
            size = root.getChildren().size();
            lastNode = root.getChildren().get(size - 1);

            String lastNodeLabel = lastNode.getLabel();
            lastNode.deselect();
            lastNode.select();

            getDriver().waitUntilElementDisappears(exportModal.getContainer(), By.linkText(lastNodeLabel));
        }

        assertEquals(50, root.getChildren().size());

        otherFormatPane.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome",
            "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatPane.getForm().getAttribute("action"));

        List<String> expectedPages = new ArrayList<>();
        expectedPages.add("xwiki:Foo.%");
        assertEquals(expectedPages, otherFormatPane.getPagesValues());

        List<String> expectedExcludes = new ArrayList<>();
        expectedExcludes.add("");
        assertEquals(expectedExcludes, otherFormatPane.getExcludesValues());

        getUtil().forceGuestUser();
    }

    /*
        Scenario: Export a XAR after opening main tree and a subtree and unselecting
        nodes including the pagination node of the subtree.
     */
    public void exportXARWithUnselect()
    {
        getUtil().loginAsSuperAdmin();
        ViewPage viewPage = getUtil().gotoPage("Foo", "WebHome");
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        ExportModal exportModal = new ExportModal();
        OtherFormatPane otherFormatPane = exportModal.openOtherFormatPane();
        assertTrue(otherFormatPane.isTreeAvailable());
        assertTrue(otherFormatPane.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatPane.getTreeElement();
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


        otherFormatPane.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome",
            "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatPane.getForm().getAttribute("action"));

        List<String> expectedPages = new ArrayList<>();
        expectedPages.add("xwiki:Foo.%");
        expectedPages.add("xwiki:Foo.Foo_10.Foo_10_1.WebHome");
        expectedPages.add("xwiki:Foo.Foo_10.Foo_10_15.WebHome");

        List<String> expectedExcludes = new ArrayList<>();
        expectedExcludes.add("xwiki%3AFoo.Foo_10.%25&xwiki%3AFoo.Foo_12.WebHome&xwiki%3AFoo.Foo_14.WebHome");
        // we must keep the same number of argument as for pages
        expectedExcludes.add("");
        expectedExcludes.add("");

        assertEquals(expectedPages, otherFormatPane.getPagesValues());
        assertEquals(expectedExcludes, otherFormatPane.getExcludesValues());

        getUtil().forceGuestUser();
    }
}
