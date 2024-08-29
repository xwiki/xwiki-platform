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
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage.createNewApplication;

/**
 * Tests the live table generator used by the AppWithinMinutes wizard.
 *
 * @version $Id$
 * @since 13.2
 * @since 12.10.6
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since 
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class LiveTableGeneratorIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    /**
     * @see "XWIKI-8616: Filter a static list in an AWM livetable does not work."
     */
    @Test
    @Order(1)
    void filterStaticList(TestUtils testUtils, TestReference testReference)
    {
        String appName = testReference.getLastSpaceReference().getName() + "App";

        // Cleanup the app space.
        testUtils.deletePage(new DocumentReference("xwiki", appName, "WebHome"), true);

        ApplicationClassEditPage classEditPage = createNewApplication(appName);

        // Create an application that has a Static List field and add a corresponding column to the live table.
        classEditPage.addField("Static List");
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep();
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
        ApplicationHomePage applicationHomePage = new ApplicationHomePage();
        LiveTableElement liveTable = applicationHomePage.getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(2, liveTable.getRowCount());
        String filterInputId = applicationHomePage.getFilterInputId(liveTable.getColumnIndex("Static List"), appName);
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
    void titleField(TestUtils testUtils, TestReference testReference)
    {
        String appName = testReference.getLastSpaceReference().getName() + "App";

        // Cleanup the app space. 
        testUtils.deletePage(new DocumentReference("xwiki", appName, "WebHome"), true);

        ApplicationClassEditPage classEditPage = createNewApplication(appName);

        // Create an application that has a Title field and add a corresponding column to the live table.
        classEditPage.addField("Title");
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep();
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
        ApplicationHomePage applicationHomePage = new ApplicationHomePage();
        LiveTableElement liveTable = applicationHomePage.getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(2, liveTable.getRowCount());
        String filterInputId = applicationHomePage.getFilterInputId(0, appName);
        liveTable.filterColumn(filterInputId, "mighty");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Location", appName + "Foo"));
        liveTable.filterColumn(filterInputId, "empty");
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("Location", appName + "Bar"));
        liveTable.filterColumn(filterInputId, "");
        assertEquals(2, liveTable.getRowCount());
    }
}
