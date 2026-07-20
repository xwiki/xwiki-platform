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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.appwithinminutes.test.po.AppWithinMinutesHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.test.po.NavigationPanel;
import org.xwiki.panels.test.po.NavigationPanelAdministrationPage;
import org.xwiki.panels.test.po.NavigationTreeElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        // Activate the Navigation Panel in the right column. We also force the right column to be displayed since
        // this test needs it and another test running before it in the same XWiki instance could have switched the
        // wiki to a layout that hides the right column (the shared AllIT instance means the page layout preference
        // is global state).
        setup.setWikiPreference("showRightPanels", "1");
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

        // Clean up the application created by this test because its top level page interferes with the navigation
        // panel administration test (which asserts the exact list of top level pages).
        AppWithinMinutesHomePage.gotoPage().deleteApplication(appName);
    }

    private ApplicationHomePage createEmptyApp(String appName)
    {
        ApplicationCreatePage appCreatePage = AppWithinMinutesHomePage.gotoPage().clickCreateApplication();
        appCreatePage.setApplicationName(appName);
        return appCreatePage.clickNextStep().clickNextStep().clickNextStep().clickFinish();
    }

    /**
     * Show the Navigation Panel in both columns so the same document nodes end up rendered by two independent trees.
     */
    @Test
    void noDuplicateIds(TestUtils setup, TestReference testReference)
        throws Exception
    {
        setup.setWikiPreference("showLeftPanels", "1");
        setup.setWikiPreference("leftPanels", "Panels.Navigation");

        String[] documentPath = {testReference.getLastSpaceReference().getName() + "NoDuplicateIds", "WebHome"};
        setup.createPage(new LocalDocumentReference(documentPath[0], documentPath[1]), "Some content");

        NavigationTreeElement leftTree = new NavigationTreeElement(
            setup.getDriver().findElement(By.cssSelector("#leftPanels .panel.Navigation .xtree")));
        leftTree.waitForIt();
        NavigationTreeElement rightTree = new NavigationTreeElement(
            setup.getDriver().findElement(By.cssSelector("#rightPanels .panel.Navigation .xtree")));
        rightTree.waitForIt();
        
        setup.getDriver().waitUntilCondition(driver -> leftTree.hasDocument(documentPath), 30);
        setup.getDriver().waitUntilCondition(driver -> rightTree.hasDocument(documentPath), 30);

        assertEquals(List.of(), getDuplicateAnchorDOMIds(setup), "Found duplicate anchor DOM ids on the page");
    }

    private List<String> getDuplicateAnchorDOMIds(TestUtils setup)
    {
        // Scoped to the tree nodes' anchors
        Map<String, Long> idCounts = setup.getDriver().findElements(By.cssSelector(".xtree a[id]")).stream()
            .map(element -> element.getAttribute("id"))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return idCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}
