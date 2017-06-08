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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Tests the live table generator used by the AppWithinMinutes wizard.
 * 
 * @version $Id$
 * @since 4.5RC1
 */
public class LiveTableGeneratorTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * The second step of the AppWithinMinutes wizard.
     */
    private ApplicationClassEditPage classEditPage;

    /**
     * The name of the application.
     */
    private String appName;

    @Before
    public void setUp() throws Exception
    {
        appName = RandomStringUtils.randomAlphabetic(6);
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();
        classEditPage = appCreatePage.clickNextStep();
    }

    /**
     * @see "XWIKI-8616: Filter a static list in an AWM livetable does not work."
     */
    @Test
    public void filterStaticList()
    {
        // Create an application that has a Static List field and add a corresponding column to the live table.
        classEditPage.addField("Static List");
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep().waitUntilPageIsLoaded();
        homeEditPage.addLiveTableColumn("Static List");

        // Add first entry.
        EntryNamePane entryNamePane = homeEditPage.clickFinish().clickAddNewEntry();
        entryNamePane.setName("Foo");
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("staticList1", "value1");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);

        // Add second entry.
        entryNamePane = new ApplicationHomePage().clickAddNewEntry();
        entryNamePane.setName("Bar");
        entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("staticList1", "value2");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);

        // Filter the Static List column of the live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        Assert.assertEquals(2, liveTable.getRowCount());
        String filterInputId = getFilterInputId(liveTable.getColumnIndex("Static List"));
        liveTable.filterColumn(filterInputId, "Second Choice");
        Assert.assertEquals(1, liveTable.getRowCount());
        Assert.assertTrue(liveTable.hasRow("Page Title", "Bar"));
        liveTable.filterColumn(filterInputId, "First Choice");
        Assert.assertEquals(1, liveTable.getRowCount());
        Assert.assertTrue(liveTable.hasRow("Page Title", "Foo"));
        liveTable.filterColumn(filterInputId, "All");
        Assert.assertEquals(2, liveTable.getRowCount());
    }

    /**
     * @see "XWIKI-8728: AWM home page does not list entries when \"Title\" column is set to be the first one"
     */
    @Test
    public void titleField()
    {
        // Create an application that has a Title field and add a corresponding column to the live table.
        classEditPage.addField("Title");
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep().waitUntilPageIsLoaded();
        homeEditPage.addLiveTableColumn("Title");
        homeEditPage.moveLiveTableColumnBefore("Title", "Page Title");

        // Add first entry.
        EntryNamePane entryNamePane = homeEditPage.clickFinish().clickAddNewEntry();
        entryNamePane.setName("Foo");
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setTitle("The Mighty Foo");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);

        // Add second entry.
        entryNamePane = new ApplicationHomePage().clickAddNewEntry();
        entryNamePane.setName("Bar");
        entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setTitle("The Empty Bar");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);

        // Filter the Title column of the live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        Assert.assertEquals(2, liveTable.getRowCount());
        String filterInputId = getFilterInputId(0);
        liveTable.filterColumn(filterInputId, "mighty");
        Assert.assertEquals(1, liveTable.getRowCount());
        Assert.assertTrue(liveTable.hasRow("Location", appName + "Foo"));
        liveTable.filterColumn(filterInputId, "empty");
        Assert.assertEquals(1, liveTable.getRowCount());
        Assert.assertTrue(liveTable.hasRow("Location", appName + "Bar"));
        liveTable.filterColumn(filterInputId, "");
        Assert.assertEquals(2, liveTable.getRowCount());
    }

    /**
     * @param columnIndex the column index
     * @return the id of the filter input for the specified column
     */
    private String getFilterInputId(int columnIndex)
    {
        return String.format("xwiki-livetable-%s-filter-%s", appName.toLowerCase(), columnIndex + 1);
    }
}
