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
package org.xwiki.test.ui;

import java.util.List;

import org.xwiki.like.test.po.LikeButton;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Execute upgrade tests.
 * 
 * @version $Id$
 */
public class Upgrade1210CustomTest extends UpgradeTest
{
    @Override
    protected void postUpdateValidate() throws Exception
    {
        // Make sure the Solr migration went as planned
        assertSolrMigration();
    }

    private void assertSolrMigration() throws Exception
    {
        // We expect the page Test.Rating to be liked by users "Admin" and "user1"
        getUtil().gotoPage(new LocalDocumentReference(List.of("Test", "Rating"), "WebHome"));
        LikeButton likeButton = new LikeButton();
        assertEquals(2, likeButton.getLikeNumber());

        getUtil().loginAsAdmin();
        // We expect the user "user1" to have a notification associated
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki.user1", "xwiki", 1);
    }

    @Override
    protected void setupLogs()
    {
        // The migration increasing the columns size to 768 skips the columns declared in lowercase on HSQLDB and
        // Oracle, such as the ones of the activity stream table of this old version: its page column keeps its size of
        // 255, so storing the event of the page with 768 characters in its reference saved by UpgradeTest fails. The
        // fix (XWIKI-25211: The migration increasing the columns size to 768 skips the columns declared in lowercase on
        // HSQLDB and Oracle) is not on this branch since it adds a risky migration for a problem that only happens when
        // upgrading an old instance on HSQLDB or Oracle.
        validateConsole.getLogCaptureConfiguration()
            .registerExcludes("size limit: 255 table: ACTIVITYSTREAM_EVENTS column: ASE_PAGE");
    }
}
