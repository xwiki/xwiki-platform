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
package org.xwiki.index.test.ui;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.xwiki.index.test.po.AllDocsPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.EntityTreeElement;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Tests for the AllDocs page.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class AllDocsTest extends AbstractTest
{
    @Test
    public void tableViewTabActions() throws Exception
    {
        // Test 1: Verify that the Action column is displayed only for logged in users
        // Create a test user
        getUtil().createUser(getClass().getSimpleName() + "_" + getTestMethodName(), "password");
        AllDocsPage page = AllDocsPage.gotoPage();
        LiveTableElement livetable = page.clickIndexTab();
        assertTrue("No Actions column found", livetable.hasColumn("Actions"));
        // Logs out to be guest
        page.logout();
        livetable = page.clickIndexTab();
        assertFalse("Actions column shouldn't be visible for guests", livetable.hasColumn("Actions"));

        // Test 2: Verify filtering works by filtering on the document name
        livetable = page.clickIndexTab();
        livetable.filterColumn("xwiki-livetable-alldocs-filter-1", "XWikiAllGroup");
        assertTrue(livetable.hasRow("Page", "XWikiAllGroup"));
    }

    @Test
    public void recycleBinTab() throws Exception
    {
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();

        AllDocsPage page = AllDocsPage.gotoPage();
        assertTrue("Deleted documents tab is not visible to Admin", page.hasDeletedDocsTab());
        assertTrue("Deleted attachments tab is not visible to Admin", page.hasDeletedAttachmentsTab());
        // Logs out to be guest
        page.logout();
        assertFalse("Deleted documents shouldn't be visible to guests", page.hasDeletedDocsTab());
        assertFalse("Deleted attachments shouldn't be visible to guests", page.hasDeletedAttachmentsTab());

    }

    /**
     * Validate the tree view.
     */
    @Test
    public void treeViewTab()
    {
        // Create a tree structure.
        String spaceName = getTestMethodName();
        getUtil().createPage(spaceName, "WebHome", null, "Grandparent Page");
        getUtil().createPage(spaceName, "Parent", null, "Parent Page", null, spaceName + ".WebHome");
        AttachmentsPane attachmentsPane =
            getUtil().createPage(spaceName, "Child", null, "Child Page", null, spaceName + ".Parent")
                .openAttachmentsDocExtraPane();
        attachmentsPane.setFileToUpload(getClass().getResource("/file.txt").getPath());
        attachmentsPane.waitForUploadToFinish("file.txt");

        EntityTreeElement tree = AllDocsPage.gotoPage().clickTreeTab();

        tree.waitForSpace(spaceName);
        assertFalse(tree.hasPage(spaceName, "WebHome", "Grandparent Page"));
        assertFalse(tree.hasPage(spaceName, "Parent", "Parent Page"));
        assertFalse(tree.hasPage(spaceName, "Child", "Child Page"));
        assertFalse(tree.hasAttachment(spaceName, "Child", "file.txt"));

        tree.lookupEntity(spaceName + ".Child@file.txt");
        tree.waitForAttachment(spaceName, "Child", "file.txt", true);
        assertTrue(tree.hasPage(spaceName, "WebHome", "Grandparent Page"));
        assertTrue(tree.hasPage(spaceName, "Parent", "Parent Page"));
        assertTrue(tree.hasPage(spaceName, "Child", "Child Page"));

        if (getDriver() instanceof JavascriptExecutor) {
            JavascriptExecutor driver = (JavascriptExecutor) getDriver();

            // The "Treeview" JavaScript object is specific to the document index page.
            assertEquals("xwiki", driver.executeScript("return window.Treeview.getSelectedResourceProperty('wiki')"));
            assertEquals(spaceName, driver.executeScript("return window.Treeview.getSelectedResourceProperty('space')"));
            assertEquals("Child", driver.executeScript("return window.Treeview.getSelectedResourceProperty('name')"));
            assertEquals("file.txt",
                driver.executeScript("return window.Treeview.getSelectedResourceProperty('attachment')"));
            // The anchor is undefined in this case.
            assertNull(driver.executeScript("return window.Treeview.getSelectedResourceProperty('anchor')"));
            assertEquals(spaceName + ".Child@file.txt", driver.executeScript("return window.Treeview.getValue()"));
        }
    }

    /**
     * @see "XWIKI-5187: XWiki Explorer doesn't support very well spaces and pages with special characters in their
     *      names"
     */
    @Test
    public void treeViewTabWithSpecialCharactersInEntityNames()
    {
        // Create a tree structure.
        String spaceName = getTestMethodName() + ".0";
        String spaceRef = getTestMethodName() + "\\.0";
        getUtil().createPage(spaceName, "WebHome", null, null);
        getUtil().createPage(spaceName, "Level.1", null, null, null, spaceRef + ".WebHome");
        getUtil().createPage(spaceName, "Level{[(2)]}", null, null, null, spaceRef + ".Level\\.1");
        getUtil().createPage(spaceName, "Level@3", null, null, null, spaceRef + ".Level{[(2)]}");
        getUtil().createPage(spaceName, "End", null, null, null, spaceRef + ".Level@3");

        EntityTreeElement tree = AllDocsPage.gotoPage().clickTreeTab();
        tree.lookupEntity(spaceRef + ".End");
        tree.waitForPage(spaceName, "End", true);
        assertTrue(tree.hasPage(spaceName, "Level.1"));
        // The curly open bracket '{' is encoded in the rendered title (for security reasons?..).
        // See http://jira.xwiki.org/browse/XWIKI-7815
        assertTrue(tree.hasPage(spaceName, "Level{[(2)]}", "Level&#123;[(2)]}"));
        assertTrue(tree.hasPage(spaceName, "Level@3"));
    }

    /**
     * This test is against XWiki Enterprise XE-701 http://jira.xwiki.org/jira/browse/XE-701 (fixed in 2.5M1)
     * WARN: calling isReady() and waitUntilReady() from LiveTableElement.java inside this class fails.
     */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void attachmentsTabFiltering() throws Exception
    {
        // Create 2 pages with attachments so that this test filter returns only one.
        // Note that we need to be logged in.
        getUtil().createPage(getTestClassName(), "Page", "content1", "title1");
        getUtil().attachFile(getTestClassName(), "Page", "attachment1.txt",
            new ByteArrayInputStream("attachment content1".getBytes()), false, TestUtils.SUPER_ADMIN_CREDENTIALS);
        getUtil().createPage(getTestClassName(), "OtherPage", "content2", "title2");
        getUtil().attachFile(getTestClassName(), "OtherPage", "attachment2.txt",
            new ByteArrayInputStream("attachment content2".getBytes()), false, TestUtils.SUPER_ADMIN_CREDENTIALS);

        AllDocsPage docsPage = AllDocsPage.gotoPage();
        LiveTableElement liveTable = docsPage.clickAttachmentsTab();
        // Here we test if all the Columns are displayed
        Assert.assertTrue("No Filename column found", liveTable.hasColumn("Filename"));
        Assert.assertTrue("No Space column found", liveTable.hasColumn("Space"));
        Assert.assertTrue("No Date column found", liveTable.hasColumn("Date"));
        Assert.assertTrue("No Author column found", liveTable.hasColumn("Author"));
        Assert.assertTrue("No Type column found", liveTable.hasColumn("Type"));
        Assert.assertTrue("No Page column found", liveTable.hasColumn("Page"));

        // Here we filter the livetable
        liveTable.filterColumn("xwiki-livetable-allattachments-filter-2", "th");
        List<WebElement> pageResults = getDriver().findElements(By.xpath("//td[@class='pagename']"));
        Assert.assertEquals(1, pageResults.size());

        // Here we get the results that remain after applying the filter
        // and we check if there is a result that doesn't contain the filter, the test will fail
        for (int i = 0; i < pageResults.size(); i++) {
            String text = pageResults.get(i).getText();
            Assert.assertTrue("This [" + text + "] should not be here !", text.toLowerCase().contains("th"));
        }
    }
}
