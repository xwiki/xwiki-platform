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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.panels.test.po.NavigationPanelAdministrationPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to the Navigation panel administration.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
@UITest(
    properties = {
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin"
    }
)
class NavigationPanelAdministrationIT
{
    @Test
    void navigationPanelAdministration(TestUtils setup, XWikiWebDriver driver) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Reset the configuration.
        setup.deleteObject("XWiki", "XWikiPreferences", "XWiki.PinnedChildPagesClass", 0);
        setup.deletePage("PanelsCode", "NavigationConfiguration");

        // By default the AdminGroup doesn't have "admin" right, so give it since we're going to create the Admin
        // user and make it part of the Admin group and we need that Admin to have "admin" rights.
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);

        setup.createAdminUser();
        setup.loginAsAdmin();

        // Create a top level page that doesn't belong to an extension. We use special characters in the page name in
        // order to test:
        // 1. XWIKI-22885: Broken navigation panel after using pinned pages (if page name contains space)
        // 2. XWIKI-23325: Pinned pages containing the '+' character in their names are not correctly displayed on the
        //    document tree
        String denis = "D e%n+i/s";
        setup.createPage(denis, "WebHome", "", "");

        NavigationPanelAdministrationPage navPanelAdminPage = NavigationPanelAdministrationPage.gotoPage();

        // Assert the initial state. Note that we have the "XWiki" space listed because we created the Admin user and
        // the XWikiAdminGroup group, both are located in the "XWiki" space and not hidden.
        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertFalse(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        // Exclude top level extension pages that are not meant to be modified.
        navPanelAdminPage.excludeTopLevelExtensionPages(true);

        assertEquals(Arrays.asList("Alice", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Arrays.asList("Alice", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Include Bob although it's a top level extension page.
        navPanelAdminPage.include("Bob");

        // Exclude Alice, Denis & XWiki.
        navPanelAdminPage.exclude(denis);
        navPanelAdminPage.exclude("Alice");
        navPanelAdminPage.exclude("XWiki");

        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getInclusions());
        assertEquals(Arrays.asList(denis, "Alice", "XWiki"), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Exclude Bob.
        navPanelAdminPage.exclude("Bob");

        assertEquals(Collections.singletonList("No pages found"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Arrays.asList(denis, "Alice", "XWiki"), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Collections.singletonList("No pages found"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());

        navPanelAdminPage.include("Alice");
        navPanelAdminPage.excludeTopLevelExtensionPages(false);
        navPanelAdminPage.include(denis);
        navPanelAdminPage.include("XWiki");

        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertFalse(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Verify multiple selection.
        navPanelAdminPage.exclude("Bob", denis);
        assertEquals(Arrays.asList("Alice", "XWiki"), navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Enable Top Level Extension Pages filter to check what happens when Bob is duplicated (explicit exclude in
        // "Other Pages" and implicit exclude by the dynamic Top Level Extension Pages filter).
        navPanelAdminPage.excludeTopLevelExtensionPages(true);

        assertEquals(Arrays.asList("Alice", "XWiki"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Arrays.asList("Bob", denis), navPanelAdminPage.getExclusions());

        // Verify multiple selection and also the fact that Bob is removed from explicit exclusions.
        navPanelAdminPage.include(denis, "Bob");

        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        // Check pin pages behaviour
        assertFalse(navPanelAdminPage.isPinned("Alice"));
        assertFalse(navPanelAdminPage.isPinned("Bob"));
        navPanelAdminPage.pinPage("Bob");

        assertTrue(navPanelAdminPage.isPinned("Alice"));
        assertTrue(navPanelAdminPage.isPinned("Bob"));

        saveAndReload(navPanelAdminPage, driver);
        assertTrue(navPanelAdminPage.isPinned("Alice"));
        assertTrue(navPanelAdminPage.isPinned("Bob"));

        navPanelAdminPage.unpinPage("Alice");
        assertFalse(navPanelAdminPage.isPinned("Alice"));
        assertFalse(navPanelAdminPage.isPinned("Bob"));

        navPanelAdminPage.dragBefore(denis, "Bob");
        assertTrue(navPanelAdminPage.isPinned("Alice"));
        assertTrue(navPanelAdminPage.isPinned(denis));
        assertFalse(navPanelAdminPage.isPinned("Bob"));

        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Arrays.asList("Alice", denis, "Bob", "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isPinned("Alice"));
        assertTrue(navPanelAdminPage.isPinned(denis));
        assertFalse(navPanelAdminPage.isPinned("Bob"));

        navPanelAdminPage.unpinPage("Alice");
        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());

        navPanelAdminPage.pinPage(denis);
        saveAndReload(navPanelAdminPage, driver);
        assertEquals(Arrays.asList("Alice", "Bob", denis, "XWiki"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isPinned(denis));

        // Reset the state of pinned page so that automated accessibility tests don't hit an unexpected fail
        // in the test following this one.
        navPanelAdminPage.unpinPage("Alice");
        navPanelAdminPage.save();
    }

    private NavigationPanelAdministrationPage saveAndReload(NavigationPanelAdministrationPage navPanelAdminPage,
        XWikiWebDriver driver)
    {
        navPanelAdminPage.save();
        driver.navigate().refresh();
        return new NavigationPanelAdministrationPage();
    }
}
