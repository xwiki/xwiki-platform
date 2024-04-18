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
package org.xwiki.menu.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.application.test.po.ApplicationIndexHomePage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.menu.test.po.MenuEntryEditPage;
import org.xwiki.menu.test.po.MenuHomePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Various Menu tests to prove that the Menu feature works (add a new menu after the header, add a left panel menu, add
 * a right panel menu, etc).
 *
 * @version $Id$
 * @since 10.6RC1
 */
// Examples of various configurations:
//@UITest(database = Database.MYSQL, databaseTag = "5", servletEngine = ServletEngine.TOMCAT, servletEngineTag = "8",
//  browser = Browser.CHROME, verbose = true)
//@UITest(database = Database.POSTGRESQL, databaseTag = "9", servletEngine = ServletEngine.JETTY,
//  servletEngineTag = "9", browser = Browser.CHROME, verbose = true)
//@UITest(database = Database.HSQLDB_EMBEDDED, servletEngine = ServletEngine.JETTY_STANDALONE,
//  browser = Browser.FIREFOX, verbose = true)
@UITest
class MenuIT
{
    @Test
    @Order(1)
    void verifyMenuInApplicationsIndex(TestUtils setup)
    {
        // Log in as superadmin
        setup.loginAsSuperAdmin();

        ApplicationIndexHomePage applicationIndexHomePage = ApplicationIndexHomePage.gotoPage();

        assertTrue(applicationIndexHomePage.containsApplication("Menu"));
        ViewPage vp = applicationIndexHomePage.clickApplication("Menu");

        // Verify we're on the right page!
        assertEquals(MenuHomePage.getSpace(), vp.getMetaDataValue("space"));
        assertEquals(MenuHomePage.getPage(), vp.getMetaDataValue("page"));

        // Now log out to verify that the Menu entry is not displayed for guest users
        setup.forceGuestUser();
        // Navigate again to the Application Index page to perform the verification
        applicationIndexHomePage = ApplicationIndexHomePage.gotoPage();
        assertFalse(applicationIndexHomePage.containsApplication("Menu"));
    }

    @Test
    @Order(2)
    void verifyMenuCreationInLeftPanelWithCurrentWikiVisibility(TestUtils setup)
    {
        // Log in as superadmin again
        setup.loginAsSuperAdmin();

        DocumentReference menu1Reference = new DocumentReference("xwiki", Arrays.asList("Menu", "menu1"), "WebHome");
        setup.deletePage(menu1Reference);

        // Navigate to the menu app home page
        MenuHomePage mhp = MenuHomePage.gotoPage();

        // Create a menu entry
        EntryNamePane pane = mhp.clickAddNewEntry();
        pane.setName("menu1");
        pane.clickAdd();
        MenuEntryEditPage meep = new MenuEntryEditPage();

        // Set the menu location to be left panels and the visibility to be WIKI
        meep.setLocation("Inside a Left Panel");
        meep.setVisibility("Current Wiki");
        meep.clickSaveAndView();

        // Now modify the Left Panels list to include the new menu since this is not automatic for the moment
        setup.updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0,
            "leftPanels", "Panels.Applications,Panels.Navigation,Menu.menu1.WebHome");

        // Verify that the menu is displayed inside left panels
        mhp = MenuHomePage.gotoPage();
        assertTrue(mhp.hasLeftPanel("menu1"));

        // Verify that the menu is displayed in the entries tables.
        TableLayoutElement tableLayout = mhp.getLiveData().getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithLink("Location", "menu1", setup.getURL(menu1Reference.getLastSpaceReference()));
        tableLayout.assertRow("Update date", hasItem(tableLayout.getDatePatternMatcher()));
        tableLayout.assertCellWithLink("Last Author", "superadmin",
            setup.getURL(new DocumentReference("xwiki", "XWiki", "superadmin")));
        tableLayout.assertCellWithEditAction("Actions", menu1Reference);
        tableLayout.assertCellWithDeleteAction("Actions", menu1Reference);
    }

    @Test
    @Order(3)
    void verifyMenuIsAvailableInAdministration(TestUtils setup) throws Exception
    {
        // Log in as superadmin
        setup.loginAsSuperAdmin();

        DocumentReference menu1Reference = new DocumentReference("xwiki", Arrays.asList("Menu", "menu1"), "WebHome");
        setup.rest().delete(menu1Reference);

        AdministrationPage administrationPage = AdministrationPage.gotoPage();

        // check that the look & feel category contains a Menu section
        assertTrue(administrationPage.hasSection("Look & Feel", "Menus"));

        administrationPage.clickSection("Look & Feel", "Menus");

        // after having clicked on the menu section, we are in the menu home page
        MenuHomePage menuPage = new MenuHomePage();

        // check that we are still in the administration
        assertEquals(AdministrationPage.getSpace(), menuPage.getMetaDataValue("space"));
        assertEquals(AdministrationPage.getPage(), menuPage.getMetaDataValue("page"));

        // Create a menu entry
        EntryNamePane pane = menuPage.clickAddNewEntry();
        pane.setName("menu1");
        pane.clickAdd();

        // check that we are now on the menu app
        assertEquals(MenuHomePage.getSpace() + ".menu1", menuPage.getMetaDataValue("space"));
        assertEquals("WebHome", menuPage.getMetaDataValue("page"));
        MenuEntryEditPage meep = new MenuEntryEditPage();

        // Set the menu location to be left panels and the visibility to be WIKI
        meep.setLocation("Inside a Left Panel");
        meep.setVisibility("Current Wiki");
        meep.clickSaveAndView();

        // Now modify the Left Panels list to include the new menu since this is not automatic for the moment
        setup.updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0,
            "leftPanels", "Panels.Applications,Panels.Navigation,Menu.menu1.WebHome");

        // Verify that the menu is displayed inside left panels
        administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasLeftPanel("menu1"));
    }

    @Test
    @Order(4)
    void testAlert(TestUtils testUtils) throws InterruptedException
    {
        ViewPage viewPage = testUtils.gotoPage("Main", "WebHome");
        testUtils.getDriver().executeJavascript("window.onbeforeunload = function () { return false; }");
        viewPage.edit();
        testUtils.getDriver().waitUntilCondition(ExpectedConditions.alertIsPresent());
        testUtils.getDriver().switchTo().alert().dismiss();
    }
}
