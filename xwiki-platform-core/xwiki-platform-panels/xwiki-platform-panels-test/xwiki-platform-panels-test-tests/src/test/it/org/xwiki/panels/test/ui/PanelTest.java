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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.panels.test.po.PageLayoutTabContent;
import org.xwiki.panels.test.po.PageWithPanels;
import org.xwiki.panels.test.po.PanelEditPage;
import org.xwiki.panels.test.po.PanelsAdministrationPage;
import org.xwiki.panels.test.po.PanelsHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.EditRightsPane.Right;
import org.xwiki.test.ui.po.EditRightsPane.State;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.RightsEditPage;
import org.xwiki.text.StringUtils;

import static org.junit.Assert.*;

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
        assertEquals(PanelsHomePage.getSpace(), vp.getMetaDataValue("space"));
        assertEquals(PanelsHomePage.getPage(), vp.getMetaDataValue("page"));

        // Now log out to verify that the Panels entry is not displayed for non admin users
        getUtil().forceGuestUser();
        // Navigate again to the Application Panels page to perform the verification
        applicationPanel = ApplicationsPanel.gotoPage();
        assertFalse(applicationPanel.containsApplication("Panels"));
    }

    /**
     * @see "XWIKI-8591: Cannot use a panel with a name containing spaces"
     */
    @Test
    public void addPanelWithSpacesAndSymbolsInName()
    {
        // Delete panel that will be created by the test
        String panelName = "Is # & \u0163 triky\"? c:\\windows /root $util";
        getUtil().deletePage("Panels", panelName);

        // Create a panel whose name contain spaces.
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);

        // We cannot reuse the panel name since we need special escapes.
        panelEditPage.setContent(
            String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, "Is # & \\u0163 triky\\\"? c:\\\\windows /root $util",
                getTestMethodName()));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration.
        setRightPanelInAdministration("Is # & ţ triky\"? c:\\\\windows /root $util");
        getUtil().gotoPage(getTestClassName(), getTestMethodName());

        assertTrue(new PageWithPanels().hasPanel("Is # & \\u0163 triky\\\"? c:\\\\windows /root $util"));
    }

    /**
     * @see "XWIKI-9126: Show panel only if the user has the view right on the panel page"
     */
    @Test
    public void limitPanelViewRight()
    {
        // Delete panel that will be created by the test
        String panelName = getTestMethodName();
        getUtil().deletePage("Panels", panelName);

        // Create a user for the test so that we can give view rights to the panel page to that user.
        String userName = String.format("%s%s", getTestClassName(), getTestMethodName());
        getUtil().createUser(userName, "password", getUtil().getURLToNonExistentPage());

        // Create new Panel
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);
        panelEditPage.setContent(String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, panelName, "Panel content."));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration. This also proves that the Panel Admin UI is
        // displayed fine and can be modified.
        setRightPanelInAdministration(panelName);
        getUtil().gotoPage(getTestClassName(), getTestMethodName());

        // The panel should be visible for the administrator.
        assertTrue(new PageWithPanels().hasPanel(panelName));

        // Force the guest user to verify the Panel is also visible for Guests.
        getUtil().forceGuestUser();
        assertTrue(new PageWithPanels().hasPanel(panelName));

        // Login and limit the view right on the panel document to the test user.
        this.authenticationRule.authenticate();
        RightsEditPage rightsEditor = getUtil().gotoPage("Panels", panelName).editRights();
        rightsEditor.switchToUsers();
        // Explicit view right for the test user.
        rightsEditor.setRight(userName, Right.VIEW, State.ALLOW);

        // Check again the panel visibility for the test user and then for guest
        getUtil().loginAndGotoPage(userName, "password", getUtil().getURL(getTestClassName(), getTestMethodName()));
        assertTrue(new PageWithPanels().hasPanel(panelName));
        getUtil().forceGuestUser();
        assertFalse(new PageWithPanels().hasPanel(panelName));
    }

    private void setRightPanelInAdministration(String panelName)
    {
        AdministrationPage.gotoPage().clickSection("Look & Feel", "Panels");
        PanelsAdministrationPage panelsAdminPage = new PanelsAdministrationPage();
        PageLayoutTabContent pageLayout = panelsAdminPage.selectPageLayout();
        pageLayout.selectRightColumnLayout();
        String rightPanels = pageLayout.getRightPanels();
        String newPanelString = "Panels." + panelName;
        if (!rightPanels.contains(newPanelString)) {
            pageLayout.setRightPanels(StringUtils.join(new Object[] {rightPanels, newPanelString}, ','));
        }
        panelsAdminPage.clickSave();
    }
}
