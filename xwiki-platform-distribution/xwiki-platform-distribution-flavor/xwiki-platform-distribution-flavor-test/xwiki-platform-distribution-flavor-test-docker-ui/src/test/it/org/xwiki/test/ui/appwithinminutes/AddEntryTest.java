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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * Tests the process of adding new application entries.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class AddEntryTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * The page being tested.
     */
    private ApplicationHomePage homePage;

    @BeforeEach
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        testUtils.deletePage(testReference);
        Map<String, String> editQueryStringParameters = new HashMap<>();
        editQueryStringParameters.put("editor", "inline");
        editQueryStringParameters.put("template", "AppWithinMinutes.LiveTableTemplate");
        editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_class", "Panels.PanelClass");
        testUtils.gotoPage(testReference, "edit", editQueryStringParameters);
        // Wait for the page to load before clicking on the save button to make sure the page layout is stable.
        this.homePage = new ApplicationHomeEditPage().waitUntilPageIsLoaded().clickSaveAndView();
    }

    /**
     * Tests that entry name is URL encoded.
     */
    @Test
    @Order(1)
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    void testEntryNameWithURLSpecialCharacters(TestUtils testUtils, TestReference testReference)
    {
        EntryNamePane entryNamePane = this.homePage.clickAddNewEntry();
        String entryName = "A?b=c&d#" + RandomStringUtils.randomAlphanumeric(3);
        entryNamePane.setName(entryName);
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("description", "This is a test panel.");
        entryEditPage.clickSaveAndView();

        testUtils.gotoPage(testReference);
        this.homePage = new ApplicationHomePage();
        LiveTableElement entriesLiveTable = this.homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        // The column header is not translated because we haven't generated the document translation bundle.
        assertTrue(entriesLiveTable.hasRow("panel.livetable.doc.title", entryName));
    }
}
