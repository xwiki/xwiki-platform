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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.release.test.po.ReleaseHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * UI tests for the Release application.
 * 
 * @version $Id$
 * @since 5.0M1
 */
public class ReleaseTest extends AbstractTest
{
    /**
     * Note: we use a dot in the release version name to verify it's supported by the Release application
     */
    private static final String RELEASE_VERSION = "4.5.1";

    /**
     * Name of the Release page that will be created.
     */
    private static final String RELEASE_PAGE_NAME = "Release" + RELEASE_VERSION.replaceAll("\\.", "");

    @Test
    public void testRelease()
    {
        // Create a user and log in with it so that we test the application with a standard user
        // Note that using the superadmin user would also fail since the uservatar macro doesn't work with it.
        getUtil().createUserAndLogin(getTestClassName() + "User", "password");

        // Delete pages that we create in the test (we have to be logged in).
        getUtil().deletePage("Release", RELEASE_PAGE_NAME);

        // Navigate to the Release app by clicking in the Application Panel.
        // This verifies that the Release application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Release");

        // Verify we're on the right page!
        Assert.assertEquals(ReleaseHomePage.getSpace(), vp.getMetaDataValue("space"));
        Assert.assertEquals(ReleaseHomePage.getPage(), vp.getMetaDataValue("page"));
        ReleaseHomePage homePage = new ReleaseHomePage();

        // Add new Release
        InlinePage entryPage = homePage.addRelease(RELEASE_VERSION);
        entryPage.waitUntilPageIsLoaded();
        vp = entryPage.clickSaveAndView();

        // Go back to the home page by clicking in the breadcrumb
        vp.clickBreadcrumbLink("Releases");
        homePage.waitUntilPageIsLoaded();

        // Assert Livetable:
        // - verify that the Translation has been applied by checking the Translated livetable column name
        // - verify that the Livetable contains our new Release entry
        LiveTableElement lt = homePage.getReleaseLiveTable();
        Assert.assertTrue(lt.hasRow("Version", RELEASE_VERSION));
    }
}
