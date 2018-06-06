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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.panels.test.po.NavigationPanelAdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to the Navigation panel administration.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
public class NavigationPanelAdministrationTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testNavigationPanelAdministration() throws Exception
    {
        // Reset the configuration.
        getUtil().deletePage("PanelsCode", "NavigationConfiguration");

        // Create a top level page that doesn't belong to an extension.
        getUtil().createPage("Denis", "WebHome", "", "");

        NavigationPanelAdministrationPage navPanelAdminPage = NavigationPanelAdministrationPage.gotoPage();

        // Assert the initial state.
        assertEquals(Arrays.asList("Alice", "Bob", "Denis"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertFalse(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        // Exclude top level extension pages that are not meant to be modified.
        navPanelAdminPage.excludeTopLevelExtensionPages(true);

        assertEquals(Arrays.asList("Alice", "Denis"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage);
        assertEquals(Arrays.asList("Alice", "Denis"), navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Include Bob although it's a top level extension page.
        navPanelAdminPage.include("Bob");

        // Exclude Alice and Denis.
        navPanelAdminPage.exclude("Denis");
        navPanelAdminPage.exclude("Alice");

        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getInclusions());
        assertEquals(Arrays.asList("Denis", "Alice"), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage);
        assertEquals(Collections.singletonList("Bob"), navPanelAdminPage.getNavigationTree().getTopLevelPages());

        // Exclude Bob.
        navPanelAdminPage.exclude("Bob");

        assertEquals(Collections.singletonList("No pages found"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertTrue(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Arrays.asList("Denis", "Alice"), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage);
        assertEquals(Collections.singletonList("No pages found"),
            navPanelAdminPage.getNavigationTree().getTopLevelPages());

        navPanelAdminPage.include("Alice");
        navPanelAdminPage.excludeTopLevelExtensionPages(false);
        navPanelAdminPage.include("Denis");

        assertEquals(Arrays.asList("Alice", "Bob", "Denis"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
        assertFalse(navPanelAdminPage.isExcludingTopLevelExtensionPages());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getInclusions());
        assertEquals(Collections.emptyList(), navPanelAdminPage.getExclusions());

        saveAndReload(navPanelAdminPage);
        assertEquals(Arrays.asList("Alice", "Bob", "Denis"), navPanelAdminPage.getNavigationTree().getTopLevelPages());
    }

    private NavigationPanelAdministrationPage saveAndReload(NavigationPanelAdministrationPage navPanelAdminPage)
    {
        navPanelAdminPage.save();
        getDriver().navigate().refresh();
        return new NavigationPanelAdministrationPage().waitUntilPageIsLoaded();
    }
}
