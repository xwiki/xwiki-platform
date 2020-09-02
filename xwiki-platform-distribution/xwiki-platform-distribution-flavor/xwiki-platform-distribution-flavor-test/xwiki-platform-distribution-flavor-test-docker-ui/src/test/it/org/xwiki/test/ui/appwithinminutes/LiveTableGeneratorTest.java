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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * Tests the live table generator used by the AppWithinMinutes wizard.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class LiveTableGeneratorTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * The second step of the AppWithinMinutes wizard.
     */
    private ApplicationClassEditPage classEditPage;

    /**
     * The name of the application.
     */
    private String appName;

    @BeforeEach
    void setUp()
    {
        this.appName = RandomStringUtils.randomAlphabetic(6);
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(this.appName);
        appCreatePage.waitForApplicationNamePreview();
        this.classEditPage = appCreatePage.clickNextStep();
    }

    /**
     * @see "XWIKI-8616: Filter a static list in an AWM livetable does not work."
     */
    @Test
    @Order(1)
    void filterStaticList()
    {
        // Create an application that has a Static List field and add a corresponding column to the live table.
        this.classEditPage.addField("Static List");
        ApplicationHomeEditPage homeEditPage =
            this.classEditPage.clickNextStep().clickNextStep().waitUntilPageIsLoaded();
        homeEditPage.addLiveTableColumn("Static List");

        // Add first entry.
        EntryNamePane entryNamePane = homeEditPage.clickFinish().clickAddNewEntry();
        entryNamePane.setName("Foo");
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("staticList1", "value1");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(this.appName);

        // Add second entry.
        entryNamePane = new ApplicationHomePage().clickAddNewEntry();
        entryNamePane.setName("Bar");
        entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setValue("staticList1", "value2");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(this.appName);

        // Filter the Static List column of the live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(2, liveTable.getRowCount());
        String filterInputId = getFilterInputId(liveTable.getColumnIndex("Static List"));
        liveTable.filterColumn(filterInputId, "Second Choice");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Page Title", "Bar"));
        liveTable.filterColumn(filterInputId, "First Choice");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Page Title", "Foo"));
        liveTable.filterColumn(filterInputId, "All");
        assertEquals(2, liveTable.getRowCount());
    }

    /**
     * @see "XWIKI-8728: AWM home page does not list entries when \"Title\" column is set to be the first one"
     */
    @Test
    @Order(2)
    void titleField()
    {
        // Create an application that has a Title field and add a corresponding column to the live table.
        this.classEditPage.addField("Title");
        ApplicationHomeEditPage homeEditPage =
            this.classEditPage.clickNextStep().clickNextStep().waitUntilPageIsLoaded();
        homeEditPage.addLiveTableColumn("Title");
        homeEditPage.moveLiveTableColumnBefore("Title", "Page Title");

        // Add first entry.
        EntryNamePane entryNamePane = homeEditPage.clickFinish().clickAddNewEntry();
        entryNamePane.setName("Foo");
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setTitle("The Mighty Foo");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(this.appName);

        // Add second entry.
        entryNamePane = new ApplicationHomePage().clickAddNewEntry();
        entryNamePane.setName("Bar");
        entryEditPage = entryNamePane.clickAdd();
        entryEditPage.setTitle("The Empty Bar");
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(this.appName);

        // Filter the Title column of the live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(2, liveTable.getRowCount());
        String filterInputId = getFilterInputId(0);
        liveTable.filterColumn(filterInputId, "mighty");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Location", this.appName + "Foo"));
        liveTable.filterColumn(filterInputId, "empty");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Location", this.appName + "Bar"));
        liveTable.filterColumn(filterInputId, "");
        assertEquals(2, liveTable.getRowCount());
    }

    /**
     * @param columnIndex the column index
     * @return the id of the filter input for the specified column
     */
    private String getFilterInputId(int columnIndex)
    {
        return String.format("xwiki-livetable-%s-filter-%s", this.appName.toLowerCase(), columnIndex + 1);
    }
}
