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
package org.xwiki.panels.test.ui.docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.panels.test.po.PageLayoutTabContent;
import org.xwiki.panels.test.po.PageWithPanels;
import org.xwiki.panels.test.po.PanelEditPage;
import org.xwiki.panels.test.po.PanelsAdministrationPage;
import org.xwiki.panels.test.po.PanelsHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Various Panel tests.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@UITest
class PanelIT
{

    // TODO: The backslash character is removed from SPECIAL_CONTENT and SPECIAL_TITLE until XWIKI-18653 is fixed.
    private static final String SPECIAL_CONTENT = "Is # & \\u0163 triky\\\"? c:windows /root $util";

    private static final String SPECIAL_TITLE = "Is # & \u0163 triky\"? c:windows /root $util";

    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void verifyApplicationsPanelEntry(TestUtils testUtils)
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
        testUtils.forceGuestUser();
        // Navigate again to the Application Panels page to perform the verification
        applicationPanel = ApplicationsPanel.gotoPage();
        assertFalse(applicationPanel.containsApplication("Panels"));
    }

    /**
     * @see "XWIKI-8591: Cannot use a panel with a name containing spaces"
     */
    @Test
    @Order(2)
    void addPanelWithSpacesAndSymbolsInName(TestUtils testUtils, TestReference testReference)
    {
        String testMethodName = testReference.getLastSpaceReference().getName();
        String testClassName = testReference.getSpaceReferences().get(0).getName();

        // Delete panel that will be created by the test
        String panelName = SPECIAL_TITLE;
        testUtils.deletePage("Panels", panelName);

        // Create a panel whose name contain spaces.
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(panelName);

        // We cannot reuse the panel name since we need special escapes.
        panelEditPage.setContent(
            String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, SPECIAL_CONTENT,
                testMethodName));
        panelEditPage.clickSaveAndContinue();

        // Checks that the new panel is listed in the Panels home live data.
        PanelsHomePage panelsHomePage = PanelsHomePage.gotoPage();
        TableLayoutElement tableLayoutElement = panelsHomePage.getLiveData().getTableLayout();
        tableLayoutElement.filterColumn("Name", panelName);
        assertEquals(1, tableLayoutElement.countRows());
        tableLayoutElement.assertRow("Description", "Panel Description");
        tableLayoutElement.assertRow("Type", "view");
        tableLayoutElement.assertRow("Category", "Information");

        // Add the panel to the right column from the administration.
        setRightPanelInAdministration(SPECIAL_TITLE);
        testUtils.gotoPage(testClassName, testMethodName);

        assertTrue(new PageWithPanels().hasPanel(SPECIAL_CONTENT));
    }

    /**
     * @see "XWIKI-9126: Show panel only if the user has the view right on the panel page"
     */
    @Test
    @Order(3)
    void limitPanelViewRight(TestUtils testUtils, TestReference testReference)
    {
        String testMethodName = testReference.getLastSpaceReference().getName();
        String testClassName = testReference.getSpaceReferences().get(0).getName();

        // Delete panel that will be created by the test
        testUtils.deletePage("Panels", testMethodName);

        // Create a user for the test so that we can give view rights to the panel page to that user.
        String userName = String.format("%s%s", testClassName, testMethodName);
        testUtils.createUser(userName, "password", testUtils.getURLToNonExistentPage());

        // Create new Panel
        PanelEditPage panelEditPage = PanelsHomePage.gotoPage().createPanel(testMethodName);
        panelEditPage.setContent(String.format(PanelEditPage.DEFAULT_CONTENT_FORMAT, testMethodName, "Panel content."));
        panelEditPage.clickSaveAndContinue();

        // Add the panel to the right column from the administration. This also proves that the Panel Admin UI is
        // displayed fine and can be modified.
        setRightPanelInAdministration(testMethodName);
        testUtils.gotoPage(testClassName, testMethodName);

        // The panel should be visible for the administrator.
        assertTrue(new PageWithPanels().hasPanel(testMethodName));

        // Force the guest user to verify the Panel is also visible for Guests.
        testUtils.forceGuestUser();
        assertTrue(new PageWithPanels().hasPanel(testMethodName));

        // Login and limit the view right on the panel document to the test user.
        testUtils.loginAsSuperAdmin();
        testUtils.setRights(new DocumentReference("xwiki", "Panels", testMethodName), null, "XWiki." + userName,
            "view", true);

        // Check again the panel visibility for the test user and then for guest
        testUtils.loginAndGotoPage(userName, "password", testUtils.getURL(testClassName, testMethodName));
        assertTrue(new PageWithPanels().hasPanel(testMethodName));
        testUtils.forceGuestUser();
        assertFalse(new PageWithPanels().hasPanel(testMethodName));

        // Cleanups the pages created in these tests because it is interfering with the navigation panel administration
        // tests.
        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(new DocumentReference("xwiki", "Panels", testMethodName));
        testUtils.deletePage(new DocumentReference("xwiki", "Panels", SPECIAL_TITLE));
    }

    @Test
    @Order(4)
    void togglePanels(TestUtils testUtils, TestReference testReference) throws Exception {
        testUtils.setWikiPreference("rightPanels", "Panels.QuickLinks");
        testUtils.setWikiPreference("leftPanels", "Panels.Welcome");
        testUtils.setWikiPreference("showRightPanels", "1");
        testUtils.setWikiPreference("showLeftPanels", "1");
        String testMethodName = testReference.getLastSpaceReference().getName();
        String testClassName = testReference.getSpaceReferences().get(0).getName();
        testUtils.gotoPage(testClassName, testMethodName);
        PageWithPanels panelPage = new PageWithPanels();
        // Check the initial state
        assertTrue(panelPage.hasLeftPanels());
        assertTrue(panelPage.hasRightPanels());
        assertTrue(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertTrue(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        // Toggle the left panels ON and OFF
        panelPage.togglePanel(PageWithPanels.LEFT);
        assertFalse(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertTrue(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        panelPage.togglePanel(PageWithPanels.LEFT);
        assertTrue(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertTrue(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        // Toggle the right panels OFF and ON
        panelPage.togglePanel(PageWithPanels.RIGHT);
        assertTrue(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertFalse(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        panelPage.togglePanel(PageWithPanels.RIGHT);
        assertTrue(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertTrue(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        // Toggle both panels OFF at once
        panelPage.togglePanel(PageWithPanels.LEFT);
        panelPage.togglePanel(PageWithPanels.RIGHT);
        assertFalse(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertFalse(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        // Reload the page to make sure the preference is kept in the localstorage
        testUtils.getDriver().navigate().refresh();
        assertFalse(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertFalse(panelPage.panelIsToggled(PageWithPanels.RIGHT));
        panelPage.togglePanel(PageWithPanels.LEFT);
        panelPage.togglePanel(PageWithPanels.RIGHT);
        testUtils.getDriver().navigate().refresh();
        assertTrue(panelPage.panelIsToggled(PageWithPanels.LEFT));
        assertTrue(panelPage.panelIsToggled(PageWithPanels.RIGHT));
    }

    @Test
    @Order(5)
    void resizePanels(TestUtils testUtils, TestReference testReference) throws Exception {
        testUtils.setWikiPreference("rightPanels", "Panels.QuickLinks");
        testUtils.setWikiPreference("leftPanels", "Panels.Welcome");
        testUtils.setWikiPreference("showRightPanels", "1");
        testUtils.setWikiPreference("showLeftPanels", "1");
        testUtils.setWikiPreference("rightPanelsWidth", "Medium");
        testUtils.setWikiPreference("leftPanelsWidth", "Medium");
        String testMethodName = testReference.getLastSpaceReference().getName();
        String testClassName = testReference.getSpaceReferences().get(0).getName();
        testUtils.gotoPage(testClassName, testMethodName);
        PageWithPanels panelPage = new PageWithPanels();
        assertAlmostEqualSize(200, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(200, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Test that the defaults are respected.
        testUtils.setWikiPreference("rightPanelsWidth", "Small");
        testUtils.setWikiPreference("leftPanelsWidth", "Small");
        // Reload the page with the new preferences taken into account.
        testUtils.gotoPage(testClassName, testMethodName);
        panelPage = new PageWithPanels();
        assertAlmostEqualSize(100, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(100, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        testUtils.setWikiPreference("rightPanelsWidth", "Large");
        testUtils.setWikiPreference("leftPanelsWidth", "Large");
        // Reload the page with the new preferences taken into account.
        testUtils.gotoPage(testClassName, testMethodName);
        panelPage = new PageWithPanels();
        assertAlmostEqualSize(300, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(300, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Check how the resize feature works.
        panelPage.resizePanel(PageWithPanels.LEFT, -30);
        panelPage.resizePanel(PageWithPanels.RIGHT, 30);
        assertAlmostEqualSize(270, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(330, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Check if the user preferences are kept on page reload.
        testUtils.gotoPage(testClassName, testMethodName);
        panelPage = new PageWithPanels();
        assertAlmostEqualSize(270, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(330, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Check if the user preferences are kept on page reload even when defaults have changed.
        testUtils.setWikiPreference("rightPanelsWidth", "Medium");
        testUtils.setWikiPreference("leftPanelsWidth", "Medium");
        testUtils.gotoPage(testClassName, testMethodName);
        panelPage = new PageWithPanels();
        assertAlmostEqualSize(270, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(330, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Check that the user preferences are reset when toggling the panel off and on.
        panelPage.togglePanel(PageWithPanels.RIGHT);
        panelPage.togglePanel(PageWithPanels.RIGHT);
        assertAlmostEqualSize(270, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(200, panelPage.getPanelWidth(PageWithPanels.RIGHT));
        // Check that the values are set to default if close enough to the default.
        panelPage.resizePanel(PageWithPanels.LEFT, -60);
        assertAlmostEqualSize(200, panelPage.getPanelWidth(PageWithPanels.LEFT));
        assertAlmostEqualSize(200, panelPage.getPanelWidth(PageWithPanels.RIGHT));
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
            pageLayout.setRightPanels(StringUtils.join(new Object[] { rightPanels, newPanelString }, ','));
        }
        panelsAdminPage.clickSave();
    }

    private void assertAlmostEqualSize(int expected, int actual) {
        /* We allow a margin of 2px difference due to small inconsistencies in the driver operations. */
        assertTrue(Math.abs(expected - actual) <= 2, "Expected " + expected + " but got " + actual);
    }
}
