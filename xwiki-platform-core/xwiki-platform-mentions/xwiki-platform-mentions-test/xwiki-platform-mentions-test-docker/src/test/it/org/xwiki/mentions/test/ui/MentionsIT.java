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
package org.xwiki.mentions.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of the mentions application UI.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@UITest(
    properties = {
        // Required for filters preferences
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }, resolveExtraJARs = true)
public class MentionsIT
{
    public static final String U1_USERNAME = "U1";

    public static final String USERS_PWD = "password";

    public static final String U2_USERNAME = "U2";

    /**
     * A duplicate of {@link Runnable} which allows to throw checked {@link Exception}.
     * @see  Runnable
     * @see <a href="https://www.baeldung.com/java-lambda-exceptions">Baeldung's Exceptions in Java 8 Lambda Expressions</a>.
     */
    @FunctionalInterface
    private interface RunnableErr
    {
        void run() throws Exception;
    }

    /**
     *
     * <ul>
     *     <li>Superadmin creates U1 and U2.</li>
     *     <li>U1 adds a mention to U2.</li>
     *     <li>U2 verify that she has received a notification.</li>
     * </ul>
     *
     * @param setup The test setup.
     * @param reference The test page reference.
     * @throws Exception In case of error.
     */
    @Test
    @Order(1)
    void basic(TestUtils setup, TestReference reference) throws Exception
    {
        String pageName = "Mention Test Page";
        runAsSuperAdmin(setup, () -> {
            // create the users.
            setup.createUser(U1_USERNAME, USERS_PWD, null);
            setup.createUser(U2_USERNAME, USERS_PWD, null);
        });

        runAsUser(setup, U1_USERNAME, USERS_PWD, () -> {
            setup.deletePage(reference);
            setup.createPage(reference,
                "{{mention reference=\"xwiki:XWiki.U2\" style=\"LOGIN\" anchor=\"test-mention-1\" /}}",
                pageName);
        });

        runAsUser(setup, U2_USERNAME, USERS_PWD, () -> {
            setup.gotoPage("Main", "WebHome");
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki.U2", "xwiki", 1);
            setup.gotoPage("Main", "WebHome");
            // check that a notif is well received
            NotificationsTrayPage tray = new NotificationsTrayPage();
            tray.showNotificationTray();
            assertEquals(1, tray.getNotificationsCount());
            assertEquals(1, tray.getUnreadNotificationsCount());
            assertEquals("mentions.mention", tray.getNotificationType(0));
            String notificationContent = tray.getNotificationContent(0);
            String expected = "You have received one mention.";
            assertTrue(notificationContent.contains(expected),
                String.format("Notification content should contain [%s] but is [%s].", expected, notificationContent));
        });
    }

    /**
     * Login as some user and perform some actions, then logout.
     *
     * @param setup The test setup.
     * @param username The user's login.
     * @param password The user's password.
     * @param actions The actions to be performed.
     * @throws Exception In case of errors.
     */
    private void runAsUser(TestUtils setup, String username, String password, RunnableErr actions) throws Exception
    {
        setup.login(username, password);
        actions.run();
    }

    /**
     * Login as supermadmin, perform some actions, then logout.
     *
     * @param setup The test setup.
     * @param actions Some actions.
     * @throws Exception In case of error.
     */
    private void runAsSuperAdmin(TestUtils setup, RunnableErr actions) throws Exception
    {
        setup.loginAsSuperAdmin();
        actions.run();
    }
}
