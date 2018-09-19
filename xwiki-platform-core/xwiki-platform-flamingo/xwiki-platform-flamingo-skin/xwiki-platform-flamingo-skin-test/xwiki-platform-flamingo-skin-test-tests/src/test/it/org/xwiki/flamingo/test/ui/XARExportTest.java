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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.flamingo.sking.test.po.ExportModal;
import org.xwiki.flamingo.sking.test.po.OtherFormatView;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.LogLevel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verify that the XAR export features works fine.
 *
 * @version $Id$
 * @since 10.8
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

        // Create 100 pages under that space (will be used for the test with lot of selected pages)
        for (int i = 0; i < 100; i++) {
            String name = "Foo_" + i;
            DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("Foo", name), "WebHome");
            if (!getUtil().rest().exists(documentReference)) {
                getUtil().rest().savePage(documentReference);
            }
        }
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
        //assertTrue(exportModal.isDisplayed());

        OtherFormatView otherFormatView = exportModal.openOtherFormatView();
        assertTrue(otherFormatView.isTreeAvailable());
        assertTrue(otherFormatView.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatView.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        otherFormatView.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatView.getForm().getAttribute("action"));
        assertEquals("xwiki%3AFoo.WebHome&xwiki%3AFoo.%25&xwiki%3AFoo.WebPreferences", otherFormatView.getCheckedPagesField().getAttribute("value"));
        assertTrue(otherFormatView.getUncheckedPagesField().getAttribute("value").isEmpty());
        assertEquals("false", otherFormatView.getOtherPagesField().getAttribute("value"));
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
        OtherFormatView otherFormatView = exportModal.openOtherFormatView();
        assertTrue(otherFormatView.isTreeAvailable());
        assertTrue(otherFormatView.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatView.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement root = topLevelNodes.get(0);
        root.open().waitForIt();

        assertEquals(16, root.getChildren().size());

        // change the timeout as it might take time to load all nodes
        getDriver().setTimeout(20);

        TreeNodeElement lastNode = null;
        int size = 0;
        for (int i = 15; i < 100; i += 15) {
            size = root.getChildren().size();
            lastNode = root.getChildren().get(size - 1);

            String lastNodeLabel = lastNode.getLabel();
            lastNode.deselect();
            lastNode.select();

            getDriver().waitUntilElementDisappears(exportModal.getContainer(), By.linkText(lastNodeLabel));
        }

        assertEquals(100, root.getChildren().size());

        otherFormatView.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatView.getForm().getAttribute("action"));

        String checkedPageValue = "xwiki%3AFoo.WebHome&xwiki%3AFoo.WebPreferences&";

        List<String> stringPieces = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String sb = "xwiki%3AFoo.Foo_"
                + i
                + ".WebHome"
                + "&xwiki%3AFoo.Foo_"
                + i
                + ".WebPreferences";
            stringPieces.add(sb);
        }
        Collections.sort(stringPieces);

        checkedPageValue += StringUtils.join(stringPieces, "&");

        assertEquals(checkedPageValue, otherFormatView.getCheckedPagesField().getAttribute("value"));
        assertTrue(otherFormatView.getUncheckedPagesField().getAttribute("value").isEmpty());
        assertEquals("false", otherFormatView.getOtherPagesField().getAttribute("value"));

        getUtil().forceGuestUser();
    }

    /*
        Scenario: Export a XAR after unselecting some pages
     */
    public void exportXARWithUnselect()
    {
        getUtil().loginAsSuperAdmin();
        ViewPage viewPage = getUtil().gotoPage("Foo", "WebHome");
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        ExportModal exportModal = new ExportModal();
        OtherFormatView otherFormatView = exportModal.openOtherFormatView();
        assertTrue(otherFormatView.isTreeAvailable());
        assertTrue(otherFormatView.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatView.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement root = topLevelNodes.get(0);
        root = root.open().waitForIt();

        assertEquals(16, root.getChildren().size());

        TreeNodeElement node5 = root.getChildren().get(5);
        TreeNodeElement node11 = root.getChildren().get(11);

        // nodes are ordered using alphabetical order:
        // 1 -> Foo_1
        // 2 -> Foo_10
        // ...
        // 5 -> Foo_13

        node5.deselect();

        // 11 -> Foo_19
        node11.deselect();

        // 12 -> Foo_2
        // 13 -> Foo_20

        otherFormatView.clickExportAsXARButton();

        String postURL = getUtil().getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome");
        assertEquals(postURL, otherFormatView.getForm().getAttribute("action"));

        String checkedPageValue = "xwiki%3AFoo.WebHome&xwiki%3AFoo.WebPreferences&";

        List<String> stringPieces = new ArrayList<>();

        for (int i = 0; i < 22; i++) {
            // see comment above: node 5 and 11 are corresponding to 13 and 19
            if (i != 13 && i != 19 && !(i > 2 && i < 10)) {
                String sb = "xwiki%3AFoo.Foo_"
                    + i
                    + ".WebHome"
                    + "&xwiki%3AFoo.Foo_"
                    + i
                    + ".WebPreferences";
                stringPieces.add(sb);
            }
        }

        Collections.sort(stringPieces);
        checkedPageValue += StringUtils.join(stringPieces, "&");

        String uncheckedValue = "xwiki%3AFoo.Foo_13.WebHome&xwiki%3AFoo.Foo_19.WebHome";
        assertEquals(checkedPageValue, otherFormatView.getCheckedPagesField().getAttribute("value"));
        assertEquals(uncheckedValue, otherFormatView.getUncheckedPagesField().getAttribute("value"));
        assertEquals("true", otherFormatView.getOtherPagesField().getAttribute("value"));

        getUtil().forceGuestUser();
    }


}
