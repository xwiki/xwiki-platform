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
package org.xwiki.release.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.release.test.po.ReleaseEntryEditPage;
import org.xwiki.release.test.po.ReleaseHomePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Docker tests for the Release Application.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@UITest
class ReleaseIT
{
    /**
     * Note: we use a dot in the release version name to verify it's supported by the Release application.
     */
    private static final String RELEASE_VERSION = "4.5.1";

    /**
     * Name of the Release page that will be created.
     */
    private static final String RELEASE_PAGE_NAME = "Release" + RELEASE_VERSION.replaceAll("\\.", "");

    @Test
    @Order(1)
    void release(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getSpaceReferences().get(0).getName();

        // Create a user and log in with it so that we test the application with a standard user
        // Note that using the superadmin user would also fail since the uservatar macro doesn't work with it.
        testUtils.createUserAndLogin(testClassName + "User", "password");

        // Delete pages that we create in the test (we have to be logged in).
        testUtils.deletePage("Release", RELEASE_PAGE_NAME);

        // Navigate to the Release app by clicking in the Application Panel.
        // This verifies that the Release application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Release");

        // Verify we're on the right page!
        assertEquals(ReleaseHomePage.RELEASE_SPACE, vp.getMetaDataValue("space"));
        assertEquals(ReleaseHomePage.RELEASE_PAGE, vp.getMetaDataValue("page"));
        ReleaseHomePage homePage = new ReleaseHomePage();

        // Add new Release
        ReleaseEntryEditPage entryPage = homePage.addRelease(RELEASE_VERSION);
        vp = entryPage.clickSaveAndView();

        // Go back to the home page by clicking in the breadcrumb
        vp.clickBreadcrumbLink("Releases");

        // Assert Livetable:
        // - verify that the Livetable contains our new Release entry.
        TableLayoutElement liveDataElement = homePage.getReleaseLiveData().getTableLayout();
        liveDataElement.assertRow("Version", RELEASE_VERSION);
    }
}
