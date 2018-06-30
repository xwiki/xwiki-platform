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

import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.menu.test.po.MenuEntryEditPage;
import org.xwiki.menu.test.po.MenuHomePage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Various Menu tests to prove that the Menu feature works (add a new menu after the header, add a left panel menu,
 * add a right panel menu, etc).
 *
 * @version $Id$
 * @since 10.6RC1
 */
@UITest
public class MenuTest
{
    @Test
    public void verifyMenu(TestUtils setup)
    {
        verifyMenuInApplicationsPanel(setup);
        verifyMenuCreationInLeftPanelWithCurrentWikiVisibility(setup);
    }

    private void verifyMenuInApplicationsPanel(TestUtils setup)
    {
        // Log in as superadmin
        setup.loginAsSuperAdmin();

        // Verify that the menu app is displayed in the Applications Panel
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Menu");

        // Verify we're on the right page!
        assertEquals(MenuHomePage.getSpace(), vp.getMetaDataValue("space"));
        assertEquals(MenuHomePage.getPage(), vp.getMetaDataValue("page"));

        // Now log out to verify that the Menu entry is not displayed for guest users
        setup.forceGuestUser();
        // Navigate again to the Application Menu page to perform the verification
        applicationPanel = ApplicationsPanel.gotoPage();
        assertFalse(applicationPanel.containsApplication("Menu"));

        // Log in as superadmin again for the rest of the tests
        setup.loginAsSuperAdmin();
    }

    private void verifyMenuCreationInLeftPanelWithCurrentWikiVisibility(TestUtils setup)
    {
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
    }
}
