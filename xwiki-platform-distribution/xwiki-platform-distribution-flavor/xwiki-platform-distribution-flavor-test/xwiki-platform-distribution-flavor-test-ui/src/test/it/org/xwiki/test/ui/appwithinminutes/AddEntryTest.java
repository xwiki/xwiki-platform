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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Tests the process of adding new application entries.
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class AddEntryTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * The page being tested.
     */
    private ApplicationHomePage homePage;

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        Map<String, String> editQueryStringParameters = new HashMap<String, String>();
        editQueryStringParameters.put("editor", "inline");
        editQueryStringParameters.put("template", "AppWithinMinutes.LiveTableTemplate");
        editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_class", "Panels.PanelClass");
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit", editQueryStringParameters);
        // Wait for the page to load before clicking on the save button to make sure the page layout is stable.
        homePage = new ApplicationHomeEditPage().waitUntilPageIsLoaded().clickSaveAndView();
    }

    /**
     * Tests that entry name is URL encoded.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testEntryNameWithURLSpecialCharacters()
    {
        EntryNamePane entryNamePane = homePage.clickAddNewEntry();
        String entryName = "A?b=c&d#" + RandomStringUtils.randomAlphanumeric(3);
        entryNamePane.setName(entryName);
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("description", "This is a test panel.");
        entryEditPage.clickSaveAndView();

        getUtil().gotoPage(getTestClassName(), getTestMethodName());
        homePage = new ApplicationHomePage();
        LiveTableElement entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        // The column header is not translated because we haven't generated the document translation bundle.
        Assert.assertTrue(entriesLiveTable.hasRow("panel.livetable.doc.title", entryName));
    }
}
