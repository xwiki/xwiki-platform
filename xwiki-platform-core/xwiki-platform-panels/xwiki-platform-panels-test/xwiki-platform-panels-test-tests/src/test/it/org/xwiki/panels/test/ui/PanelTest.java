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
package org.xwiki.panels.test.ui;

import org.junit.*;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.PageElementsAdministrationSectionPage;
import org.xwiki.panels.test.po.PageWithPanels;
import org.xwiki.panels.test.po.PanelEditPage;
import org.xwiki.panels.test.po.PanelsHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.EditRightsPane.Right;
import org.xwiki.test.ui.po.EditRightsPane.State;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.RightsEditPage;

/**
 * Various Panel tests.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class PanelTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil(), getDriver());

    /**
     * @see "XWIKI-8591: Cannot use a panel with a name containing spaces"
     */
    @Test
    public void addPanelWithSpacesInName()
    {
        // Create a panel whose name contain spaces.
        String panelName = "My First Panel";
        getUtil().deletePage("Panels", panelName);
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);
        panelEditPage.setContent(String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, panelName, getTestMethodName()));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration.
        PageElementsAdministrationSectionPage pageElements =
            new AdministrablePage().clickAdministerWiki().clickPageElementsSection();
        String rightPanels = pageElements.getRightPanels();
        pageElements.setRightPanels(rightPanels + ",Panels." + panelName);
        try {
            pageElements.clickSave();
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));
        } finally {
            // Restore the right panels.
            pageElements = PageElementsAdministrationSectionPage.gotoPage();
            pageElements.setRightPanels(rightPanels);
            pageElements.clickSave();
        }
    }

    /**
     * @see "XWIKI-9126: Show panel only if the user has the view right on the panel page"
     */
    @Test
    public void limitPanelViewRight()
    {
        // Create a new panel.
        String panelName = getTestMethodName();
        getUtil().deletePage("Panels", panelName);
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);
        panelEditPage.setContent(String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, panelName, "Panel content."));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration.
        PageElementsAdministrationSectionPage pageElements =
            new AdministrablePage().clickAdministerWiki().clickPageElementsSection();
        String rightPanels = pageElements.getRightPanels();
        pageElements.setRightPanels(rightPanels + ",Panels." + panelName);
        try {
            pageElements.clickSave();

            // The panel should be visible for the administrator.
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));

            // Go to a page that doesn't exist and logout to see the panel as guest.
            getUtil().gotoPage(getTestClassName(), getTestMethodName()).logout();
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));

            // Login and limit the view right on the panel document.
            this.authenticationRule.authenticate();
            RightsEditPage rightsEditor = getUtil().gotoPage("Panels", panelName).editRights();
            rightsEditor.switchToUsers();
            // Explicit view right for the administrator.
            rightsEditor.setRight("Admin", Right.VIEW, State.ALLOW);

            // Check again the panel visibility for guest and then for administrator.
            ViewPage page = getUtil().gotoPage(getTestClassName(), getTestMethodName());
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));
            page.logout();
            Assert.assertFalse(new PageWithPanels().hasPanel(panelName));
        } finally {
            // Restore the right panels.
            this.authenticationRule.authenticate();
            pageElements = PageElementsAdministrationSectionPage.gotoPage();
            pageElements.setRightPanels(rightPanels);
            pageElements.clickSave();
        }
    }
}
