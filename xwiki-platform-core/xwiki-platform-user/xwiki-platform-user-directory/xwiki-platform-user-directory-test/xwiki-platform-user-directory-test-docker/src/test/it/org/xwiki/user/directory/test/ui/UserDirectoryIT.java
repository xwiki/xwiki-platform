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
package org.xwiki.user.directory.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.user.directory.test.po.UserDirectoryPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the User Directory feature.
 *
 * @version $Id$
 * @since 11.8
 */
@UITest(
    properties = {
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml.
        // Note that the Notifications feature is drawn by the Tag UI extension that is a dependency of User Directory
        // UI dependency (it's used to display the tag cloud for the User Directory LiveTable). Ideally the Tag UI
        // extension would only optionally draw the Notifications feature, since it's used only in the UI for
        // displaying documents tagged with a given tag, where the Activity Stream for a tag is displayed too (and
        // Activity Stream is implemented with the Notifications feature).
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
        // manually. See https://jira.xwiki.org/browse/XWIKI-21594
        // We need it since the Tag UI requires Notifications, as otherwise even streams won't have a store.
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
class UserDirectoryIT
{
    @Test
    void verifyUserIsListed(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration)
    {
        setup.loginAsSuperAdmin();

        // Delete possible existing user
        setup.deletePage("XWiki", "test");

        UserDirectoryPage page = UserDirectoryPage.gotoPage();

        // Verify that the user directory is empty when there's no user in the wiki
        LiveTableElement liveTableElement = page.getUserDirectoryLiveTable();
        assertEquals(0, liveTableElement.getRowCount());

        // Add a user and verify it's visible in the livetable
        setup.createUserAndLogin("test", "testtest", "first_name", "John", "last_name", "Doe");

        // Go back to the user directory page since the user creation navigated to another page
        page = UserDirectoryPage.gotoPage();
        assertEquals(1, liveTableElement.getRowCount());
        assertTrue(liveTableElement.hasRow("User ID", "test"));
        assertTrue(liveTableElement.hasRow("First Name", "John"));
        assertTrue(liveTableElement.hasRow("Last Name", "Doe"));

        // Log out to verify the livetable works in guest view too
        setup.forceGuestUser();
        assertEquals(1, liveTableElement.getRowCount());
        assertTrue(liveTableElement.hasRow("User ID", "test"));
        assertTrue(liveTableElement.hasRow("First Name", "John"));
        assertTrue(liveTableElement.hasRow("Last Name", "Doe"));

        logCaptureConfiguration.registerExcludes(
            "Exception in macro #displayCheckedIfWatched called at",
            "Exception in macro #generateNotificationInput called at"
        );
    }
}
