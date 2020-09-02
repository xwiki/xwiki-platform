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
package org.xwiki.test.ui.appwithinminutes;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the last step of the App Within Minutes wizard.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class LiveTableEditorTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    /**
     * The page being tested.
     */
    private ApplicationHomeEditPage editPage;

    /**
     * The query string parameters passed to the edit action.
     */
    private final Map<String, String> editQueryStringParameters = new HashMap<>();

    @BeforeEach
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        testUtils.deletePage(testReference);
        this.editQueryStringParameters.put("editor", "inline");
        this.editQueryStringParameters.put("template", "AppWithinMinutes.LiveTableTemplate");
        this.editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers");
        testUtils.gotoPage(testReference, "edit", this.editQueryStringParameters);
        this.editPage = new ApplicationHomeEditPage().waitUntilPageIsLoaded();
    }

    /**
     * Adds, removes and reorders live table columns.
     */
    @Test
    @Order(1)
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    void testManageColumns()
    {
        this.editPage.addLiveTableColumn("First Name");
        assertTrue(this.editPage.hasLiveTableColumn("First Name"));
        this.editPage.moveLiveTableColumnBefore("First Name", "Location");
        this.editPage.removeLiveTableColumn("Page Title");
        assertFalse(this.editPage.hasLiveTableColumn("Page Title"));
        LiveTableElement liveTable = ((ApplicationHomePage) this.editPage.clickSaveAndView()).getEntriesLiveTable();
        liveTable.waitUntilReady();
        // The column headers aren't translated because we haven't generated the document translation bundle.
        assertFalse(liveTable.hasColumn("xwikiusers.livetable.doc.title"));
        assertEquals(0, liveTable.getColumnIndex("xwikiusers.livetable.first_name"));
        assertEquals(1, liveTable.getColumnIndex("xwikiusers.livetable.doc.location"));
    }

    /**
     * Tests that Save & Continue works fine.
     */
    @Test
    @Order(2)
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    void testSaveAndContinue()
    {
        this.editPage.setDescription("wait for WYSIWYG to load");
        this.editPage.clickSaveAndContinue();
        ApplicationHomePage viewPage = this.editPage.clickCancel();
        LiveTableElement liveTable = viewPage.getEntriesLiveTable();
        liveTable.waitUntilReady();
        // The column header isn't translated because we haven't generated the document translation bundle.
        assertTrue(liveTable.hasColumn("xwikiusers.livetable.doc.title"));
    }

    /**
     * Tests how deprecated columns are handled.
     */
    @Test
    @Order(3)
    void testDeprecatedColumns(TestUtils testUtils, TestReference testReference)
    {
        // Fake a deprecated column by using a column that doesn't exist.
        this.editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_columns", "doc.name foo");
        testUtils.gotoPage(testReference, "edit", this.editQueryStringParameters);
        this.editPage = new ApplicationHomeEditPage().waitUntilPageIsLoaded();

        assertTrue(this.editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertFalse(this.editPage.isLiveTableColumnDeprecated("Page Name"));
        assertTrue(this.editPage.isLiveTableColumnDeprecated("foo"));

        // Keep deprecated columns.
        this.editPage.removeAllDeprecatedLiveTableColumns(false);
        assertFalse(this.editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertTrue(this.editPage.isLiveTableColumnDeprecated("foo"));
        ApplicationHomePage viewPage = this.editPage.clickSaveAndView();
        LiveTableElement liveTable = viewPage.getEntriesLiveTable();
        liveTable.waitUntilReady();
        // The column header isn't translated because we haven't generated the document translation bundle.
        assertTrue(liveTable.hasColumn("xwikiusers.livetable.foo"));

        // Edit again and remove the deprecated column.
        this.editPage = viewPage.editInline();
        assertTrue(this.editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        this.editPage.removeLiveTableColumn("foo");
        assertFalse(this.editPage.hasLiveTableColumn("foo"));
        // The warning must disappear if we remove the deprecated column.
        assertFalse(this.editPage.isDeprecatedLiveTableColumnsWarningDisplayed());

        // Reload and remove all deprecated columns.
        this.editPage = viewPage.editInline();
        this.editPage.removeAllDeprecatedLiveTableColumns(true);
        assertFalse(this.editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertTrue(this.editPage.hasLiveTableColumn("Page Name"));
        assertFalse(this.editPage.hasLiveTableColumn("foo"));
    }

    /**
     * Tests that the live table isn't generated if the list of columns is empty.
     */
    @Test
    @Order(4)
    void testNoColumns(TestUtils testUtils, TestReference testReference)
    {
        // Make sure the list of columns is empty.
        this.editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_columns", "");
        testUtils.gotoPage(testReference, "edit", this.editQueryStringParameters);
        // Wait for the page to load before clicking on the save button to be sure the page layout is stable.
        ApplicationHomePage viewPage = new ApplicationHomeEditPage().waitUntilPageIsLoaded().clickSaveAndView();
        assertFalse(viewPage.hasEntriesLiveTable());
        assertEquals("", viewPage.editWiki().getContent());
    }
}
