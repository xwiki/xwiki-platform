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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.AppWithinMinutesHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.test.po.NavigationPanel;
import org.xwiki.panels.test.po.NavigationPanelAdministrationPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verify the Navigation Panel.
 * 
 * @version $Id$
 * @since 11.4RC1
 */
@UITest(properties = {
    // Remove once https://jira.xwiki.org/browse/XWIKI-20529 is fixed. Right now AWM requires PR on the following
    // docs.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\."
        + "(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class NavigationPanelIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Activate the Navigation Panel in the right column
        setup.setWikiPreference("rightPanels", "Panels.Navigation");
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-16247">XWIKI-16247: Top level applications pages are not
     *      excluded properly from Navigation Panel on Home page</a>
     */
    @Test
    void verifyPanelCaching(TestUtils setup, TestReference testReference)
    {
        // Make sure the application we're about to create doesn't exist.
        String appName = testReference.getSpaceReferences().get(0).getName() + "App";
        AppWithinMinutesHomePage.gotoPage().deleteApplication(appName);

        // Configure the Navigation Panel to exclude top level application pages.
        NavigationPanelAdministrationPage navigationPanelAdminPage = NavigationPanelAdministrationPage.gotoPage();
        navigationPanelAdminPage.excludeTopLevelApplicationPages(true);
        navigationPanelAdminPage.save();

        // Access some page to cache the Navigation Panel.
        LocalDocumentReference page = new LocalDocumentReference("Some", "Page");
        setup.gotoPage(page);

        // Create a top level application.
        createEmptyApp(appName);

        // Verify that the application home page is excluded from the Navigation Panel.
        setup.gotoPage(page);
        assertFalse(new NavigationPanel().getNavigationTree().hasDocument(appName, "WebHome"),
            "The application home page is not excluded from the Navigation Panel");
    }

    private ApplicationHomePage createEmptyApp(String appName)
    {
        ApplicationCreatePage appCreatePage = AppWithinMinutesHomePage.gotoPage().clickCreateApplication();
        appCreatePage.setApplicationName(appName);
        return appCreatePage.clickNextStep().clickNextStep().clickNextStep().clickFinish();
    }
}
