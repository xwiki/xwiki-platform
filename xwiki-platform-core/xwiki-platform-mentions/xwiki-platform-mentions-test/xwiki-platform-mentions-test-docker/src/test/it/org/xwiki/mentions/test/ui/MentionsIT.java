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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.mentions.test.po.MentionNotificationPage;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.platform.notifications.test.po.NotificationsTrayPage.waitOnNotificationCount;

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
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as an extension, so we need to add it to WEB-INF/lib
        // manually. See https://jira.xwiki.org/browse/XWIKI-21594
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr",
        // Required to ensure that the notifications rest endpoints are registered before XWikiJaxRsApplication is
        // initialized.
        "org.xwiki.platform:xwiki-platform-notifications-rest"
    },
    resolveExtraJARs = true
)
class MentionsIT
{
    private static final String U1_USERNAME = "U1";

    private static final String U2_USERNAME = "U2";

    private static final String U3_USERNAME = "U3";

    private static final String U4_USERNAME = "U4";

    private static final String USERS_PWD = "password";

    private static final String MENTION_NOTIFICATION_CONTENT = "You have received one mention.";

    @BeforeAll
    static void beforeAll(TestUtils setup) throws Exception
    {
        // Each test mentions a user of its own, so that the notifications received by one test can't be seen by
        // another one and the tests don't have to clear them.
        for (String username : new String[] { U1_USERNAME, U2_USERNAME, U3_USERNAME, U4_USERNAME }) {
            setup.rest().createUser(username, USERS_PWD);
        }
    }

    /**
     * <ul>
     *     <li>U1 mentions U2 in the content of a page.</li>
     *     <li>U2 verifies that she has received a notification.</li>
     * </ul>
     *
     * @param setup The test setup.
     * @param reference The test page reference.
     * @throws Exception In case of error.
     */
    @Test
    @Order(1)
    void documentBody(TestUtils setup, TestReference reference) throws Exception
    {
        String pageName = "Mention Test Page";
        setup.rest().delete(reference);
        setup.rest().runAs(new XWikiCredentials(U1_USERNAME, USERS_PWD), rest -> rest.savePage(reference,
            "<strong>Quote</strong> "
                + "{{mention reference=\"xwiki:XWiki.U2\" style=\"LOGIN\" anchor=\"test-mention-1\" /}}",
            pageName));

        setup.login(U2_USERNAME, USERS_PWD);
        setup.gotoPage("Main", "WebHome");
        waitOnNotificationCount("xwiki:XWiki.U2", "xwiki", 1);
        // check that a notif is well received
        NotificationsTrayPage tray = assertMentionNotification();
        final WebElement rootElement = tray.getNotificationsButton();
        MentionNotificationPage mentionNotificationPage = new MentionNotificationPage(rootElement);
        mentionNotificationPage.openGroup(0);
        assertEquals("mentioned you on page Mention Test Page", mentionNotificationPage.getText(0, 0));
        assertEquals("U1", mentionNotificationPage.getEmitter(0, 0));
        assertTrue(mentionNotificationPage.hasSummary(0, 0));
        assertEquals("<strong>Quote</strong> @U2", mentionNotificationPage.getSummary(0, 0));
    }

    /**
     * <ul>
     *     <li>U3 mentions U4 in a comment of a page created by U1.</li>
     *     <li>U4 verifies that she has received a notification.</li>
     * </ul>
     *
     * @param setup The test setup.
     * @param reference The test page reference.
     * @throws Exception In case of error.
     */
    @Test
    @Order(2)
    void comment(TestUtils setup, TestReference reference) throws Exception
    {
        String pageName = "Mention Comment Test Page";
        setup.rest().delete(reference);
        setup.rest().runAs(new XWikiCredentials(U1_USERNAME, USERS_PWD), rest -> rest.savePage(reference, "", pageName));

        // We comment with a user distinct from the one who created the page (U1) to make sure that the emitter of
        // the mention is correct. The author property of the comment is deliberately set to U1 to make sure that the
        // emitter is the user who actually added the comment and not the one declared in the comment.
        setup.rest().runAs(new XWikiCredentials(U3_USERNAME, USERS_PWD), rest -> rest.addObject(reference, "XWiki.XWikiComments",
            "author", "xwiki:XWiki.U1",
            "date", "17/08/2020 14:55:18",
            "comment", "AAAAA\n\n"
                + "<strong>Quote</strong> "
                + "{{mention reference=\"xwiki:XWiki.U4\" style=\"LOGIN\" anchor=\"test-mention-2\" "
                + "type=\"user\" /}} XYZ\n\nBBBBB"));

        setup.login(U4_USERNAME, USERS_PWD);
        setup.gotoPage("Main", "WebHome");
        waitOnNotificationCount("xwiki:XWiki.U4", "xwiki", 1);
        // check that a notif is well received
        NotificationsTrayPage tray = assertMentionNotification();
        final WebElement rootElement = tray.getNotificationsButton();
        MentionNotificationPage mentionNotificationPage = new MentionNotificationPage(rootElement);
        mentionNotificationPage.openGroup(0);
        assertEquals("mentioned you on a comment on page Mention Comment Test Page",
            mentionNotificationPage.getText(0, 0));
        assertEquals("U3", mentionNotificationPage.getEmitter(0, 0));
        assertTrue(mentionNotificationPage.hasSummary(0, 0));
        assertEquals("<strong>Quote</strong> @U4 XYZ", mentionNotificationPage.getSummary(0, 0));
    }

    /**
     * Assert that the notification tray of the currently logged in user holds exactly one unread mention
     * notification.
     *
     * @return the notification tray, with its notifications displayed
     */
    private NotificationsTrayPage assertMentionNotification()
    {
        NotificationsTrayPage tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        assertEquals(1, tray.getNotificationsCount());
        assertEquals(1, tray.getUnreadNotificationsCount());
        assertEquals("mentions.mention", tray.getNotificationType(0));
        String notificationContent = tray.getNotificationContent(0);
        assertTrue(notificationContent.contains(MENTION_NOTIFICATION_CONTENT),
            String.format("Notification content should contain [%s] but is [%s].", MENTION_NOTIFICATION_CONTENT,
                notificationContent));
        return tray;
    }
}
