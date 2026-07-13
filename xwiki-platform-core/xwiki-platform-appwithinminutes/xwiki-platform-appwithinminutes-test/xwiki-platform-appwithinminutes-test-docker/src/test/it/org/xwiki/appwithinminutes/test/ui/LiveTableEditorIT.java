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
package org.xwiki.appwithinminutes.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the last step of the App Within Minutes wizard.
 *
 * @version $Id$
 * @since 4.0M1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class LiveTableEditorIT
{
    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference, true);
    }

    /**
     * Adds, removes and reorders live table columns.
     */
    @Test
    @Order(1)
    void manageColumns(TestUtils setup, TestReference testReference)
    {
        ApplicationHomeEditPage editPage = editLiveTable(setup, testReference, null);

        editPage.addLiveTableColumn("First Name");
        assertTrue(editPage.hasLiveTableColumn("First Name"));
        editPage.moveLiveTableColumnBefore("First Name", "Location");
        editPage.removeLiveTableColumn("Page Title");
        assertFalse(editPage.hasLiveTableColumn("Page Title"));
        LiveTableElement liveTable = ((ApplicationHomePage) editPage.clickSaveAndView()).getEntriesLiveTable();
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
    void saveAndContinue(TestUtils setup, TestReference testReference)
    {
        ApplicationHomeEditPage editPage = editLiveTable(setup, testReference, null);

        editPage.setDescription("wait for WYSIWYG to load");
        editPage.clickSaveAndContinue();
        ApplicationHomePage viewPage = editPage.clickCancel();
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
    void deprecatedColumns(TestUtils setup, TestReference testReference)
    {
        // Fake a deprecated column by using a column that doesn't exist.
        ApplicationHomeEditPage editPage = editLiveTable(setup, testReference, "doc.name foo");

        assertTrue(editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertFalse(editPage.isLiveTableColumnDeprecated("Page Name"));
        assertTrue(editPage.isLiveTableColumnDeprecated("foo"));

        // Keep deprecated columns.
        editPage.removeAllDeprecatedLiveTableColumns(false);
        assertFalse(editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertTrue(editPage.isLiveTableColumnDeprecated("foo"));
        ApplicationHomePage viewPage = editPage.clickSaveAndView();
        LiveTableElement liveTable = viewPage.getEntriesLiveTable();
        liveTable.waitUntilReady();
        // The column header isn't translated because we haven't generated the document translation bundle.
        assertTrue(liveTable.hasColumn("xwikiusers.livetable.foo"));

        // Edit again and remove the deprecated column.
        editPage = viewPage.editInline();
        assertTrue(editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        editPage.removeLiveTableColumn("foo");
        assertFalse(editPage.hasLiveTableColumn("foo"));
        // The warning must disappear if we remove the deprecated column.
        assertFalse(editPage.isDeprecatedLiveTableColumnsWarningDisplayed());

        // Reload and remove all deprecated columns.
        editPage = viewPage.editInline();
        editPage.removeAllDeprecatedLiveTableColumns(true);
        assertFalse(editPage.isDeprecatedLiveTableColumnsWarningDisplayed());
        assertTrue(editPage.hasLiveTableColumn("Page Name"));
        assertFalse(editPage.hasLiveTableColumn("foo"));
    }

    /**
     * Tests that the live table isn't generated if the list of columns is empty.
     */
    @Test
    @Order(4)
    void noColumns(TestUtils setup, TestReference testReference)
    {
        // Make sure the list of columns is empty.
        editLiveTable(setup, testReference, "");
        // Wait for the page to load before clicking on the save button to be sure the page layout is stable.
        ApplicationHomePage viewPage = new ApplicationHomeEditPage().clickSaveAndView();
        assertFalse(viewPage.hasEntriesLiveTable());
        assertEquals("", viewPage.editWiki().getContent());
    }

    /**
     * Navigates to the live table editor (last step of the wizard) for the test page.
     *
     * @param columns the value of the {@code AppWithinMinutes.LiveTableClass_0_columns} property, or {@code null} to
     *            keep the template default columns
     */
    private ApplicationHomeEditPage editLiveTable(TestUtils setup, TestReference testReference, String columns)
    {
        if (columns == null) {
            setup.gotoPage(testReference, "edit", "editor", "inline",
                "template", "AppWithinMinutes.LiveTableTemplate",
                "AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers");
        } else {
            setup.gotoPage(testReference, "edit", "editor", "inline",
                "template", "AppWithinMinutes.LiveTableTemplate",
                "AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers",
                "AppWithinMinutes.LiveTableClass_0_columns", columns);
        }
        return new ApplicationHomeEditPage();
    }
}
