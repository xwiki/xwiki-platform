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
import org.xwiki.panels.test.po.ApplicationsPanel;
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
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyApplicationsPanelEntry()
    {
        // Navigate to the Panels app by clicking in the Application Panel.
        // This verifies that the Panels application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Panels");

        // Verify we're on the right page!
        Assert.assertEquals(PanelsHomePage.getSpace(), vp.getMetaDataValue("space"));
        Assert.assertEquals(PanelsHomePage.getPage(), vp.getMetaDataValue("page"));

        // Now log out to verify that the Panels entry is not displayed for non admin users
        vp.logout();
        // Navigate again to the Application Panels page to perform the verification
        applicationPanel = ApplicationsPanel.gotoPage();
        Assert.assertFalse(applicationPanel.containsApplication("Panels"));
    }

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

        // Create a user for the test so that we can give view rights to the panel page to that user.
        String userName = String.format("%s%s", getTestClassName(), getTestMethodName());
        getUtil().createUser(userName, "password", getUtil().getURLToNonExistentPage());

        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);
        panelEditPage.setContent(String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, panelName, "Panel content."));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration.
        PageElementsAdministrationSectionPage pageElements =
            new AdministrablePage().clickAdministerWiki().clickPageElementsSection();
        String rightPanels = pageElements.getRightPanels();
        pageElements.setRightPanels(rightPanels + ",Panels." + panelName);
        pageElements.clickSave();
        try {
            // The panel should be visible for the administrator.
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));

            // Go to a page that doesn't exist and logout to see the panel as guest.
            getUtil().gotoPage(getTestClassName(), getTestMethodName()).logout();
            Assert.assertTrue(new PageWithPanels().hasPanel(panelName));

            // Login and limit the view right on the panel document.
            this.authenticationRule.authenticate();
            RightsEditPage rightsEditor = getUtil().gotoPage("Panels", panelName).editRights();
            rightsEditor.switchToUsers();
            // Explicit view right for the test user.
            rightsEditor.setRight(userName, Right.VIEW, State.ALLOW);

            // Check again the panel visibility for the test user and then for guest
            getUtil().loginAndGotoPage(userName, "password", getUtil().getURL(getTestClassName(), getTestMethodName()));
            ViewPage page = new ViewPage();
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
