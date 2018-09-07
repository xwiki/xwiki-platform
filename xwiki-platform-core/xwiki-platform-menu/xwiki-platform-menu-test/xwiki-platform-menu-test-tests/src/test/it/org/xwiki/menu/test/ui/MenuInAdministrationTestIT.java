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

import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.menu.test.po.MenuEntryEditPage;
import org.xwiki.menu.test.po.MenuHomePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to check that the Menu app is available through the administration and can be used to add new entries
 *
 */
public class MenuInAdministrationTestIT extends AbstractTest
{
    @Test
    public void verifyMenuIsAvailableInAdministration() throws Exception
    {
        // Log in as superadmin
        getUtil().loginAsSuperAdmin();

        DocumentReference menu1Reference = new DocumentReference("xwiki", Arrays.asList("Menu", "menu1"), "WebHome");
        getUtil().rest().delete(menu1Reference);

        AdministrationPage administrationPage = AdministrationPage.gotoPage();

        // check that the look & feel category contains a Menu section
        assertTrue(administrationPage.hasSection("Look & Feel", "Menu"));

        administrationPage.clickSection("Look & Feel", "Menu");

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
        getUtil().updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0,
                "leftPanels", "Panels.Applications,Panels.Navigation,Menu.menu1.WebHome");

        // Verify that the menu is displayed inside left panels
        administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasLeftPanel("menu1"));
    }
}
