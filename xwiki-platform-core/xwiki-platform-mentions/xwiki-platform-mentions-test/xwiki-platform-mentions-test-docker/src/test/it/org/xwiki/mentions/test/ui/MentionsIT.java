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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.converter.EmailConverter;
import org.xwiki.http.internal.XWikiCredentials;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.mentions.test.po.MentionNotificationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal.NotificationFormat;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference.FilterAction;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CommentsTab;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.platform.notifications.test.po.NotificationsTrayPage.waitOnNotificationCount;

/**
 * Test of the mentions application UI.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        // Required for filters preferences and for sending the notification emails
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml,notification-filter-preferences.hbm.xml",
        // Required to trigger the notification email job
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.scheduler.SchedulerPlugin"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-mail-send-storage",
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

    private static final XWikiCredentials U1_CREDENTIALS = new XWikiCredentials(U1_USERNAME, USERS_PWD);

    private static final XWikiCredentials U3_CREDENTIALS = new XWikiCredentials(U3_USERNAME, USERS_PWD);

    private static final String MENTION_NOTIFICATION_CONTENT = "You have received one mention.";

    private static final String U5_USERNAME = "U5";

    private static final String U5_EMAIL = "u5@xwiki.org";

    private static final String LOCAL_USERNAME = "LocalUser";

    private static final String LOCAL_EMAIL = "local@xwiki.org";

    private static final String ADMIN_EMAIL = "admin@xwiki.org";

    private static final String MAIN_WIKI = "xwiki";

    private static final String USERS_SPACE = "XWiki";

    private static final String PAGES_APPLICATION = "org.xwiki.platform";

    private static final String MENTIONS_APPLICATION = "mentions.application.name";

    private static final String MENTION_EVENT_TYPE = "mentions.mention";

    private static final String ALERT_FORMAT = "alert";

    private static final String EMAIL_FORMAT = "email";

    private static final String DAILY_EMAIL_JOB = "Notifications daily email";

    private static final String MENTION_ON_PAGE = "mentioned you on page ";

    // Matches the first line of an event in the plain text part of a notification email, e.g.
    // "  [mentions.mention] [Some title](https://...)." and captures the event type and the document title.
    private static final Pattern PLAIN_TEXT_EVENT = Pattern.compile("^\\s*\\[([^\\]]+)\\] \\[([^\\]]*)\\]\\(",
        Pattern.MULTILINE);

    private GreenMail mail;

    @BeforeAll
    static void beforeAll(TestUtils setup) throws Exception
    {
        // Each test mentions a user of its own, so that the notifications received by one test can't be seen by
        // another one and the tests don't have to clear them.
        for (String username : new String[] { U1_USERNAME, U2_USERNAME, U3_USERNAME, U4_USERNAME }) {
            setup.rest().createUser(new XWikiCredentials(username, USERS_PWD));
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
        setup.rest().runAs(U1_CREDENTIALS, rest -> rest.savePage(reference,
            "<strong>Quote</strong> "
                + "{{mention reference=\"xwiki:XWiki.U2\" style=\"LOGIN\" anchor=\"test-mention-1\" /}}",
            pageName));

        setup.login(U2_USERNAME, USERS_PWD);
        setup.gotoPage("Main", "WebHome");
        waitOnNotificationCount("xwiki:XWiki.U2", "xwiki", 1);
        // check that a notif is well received
        NotificationsTrayPage tray = assertMentionNotification();
        MentionNotificationPage mentionNotificationPage = new MentionNotificationPage(tray);
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
        setup.rest().runAs(U1_CREDENTIALS, rest -> rest.savePage(reference, "", pageName));

        // We comment with a user distinct from the one who created the page (U1) to make sure that the emitter of
        // the mention is correct. The author property of the comment is deliberately set to U1 to make sure that the
        // emitter is the user who actually added the comment and not the one declared in the comment.
        setup.rest().runAs(U3_CREDENTIALS, rest -> {
            CommentsTab.restPostComment(reference, "AAAAA\n\n"
                    + "<strong>Quote</strong> "
                    + "{{mention reference=\"xwiki:XWiki.U4\" style=\"LOGIN\" anchor=\"test-mention-2\" "
                    + "type=\"user\" /}} XYZ\n\nBBBBB",
                "author", "xwiki:XWiki.U1", "date", "17/08/2020 14:55:18");
        });

        setup.login(U4_USERNAME, USERS_PWD);
        setup.gotoPage("Main", "WebHome");
        waitOnNotificationCount("xwiki:XWiki.U4", "xwiki", 1);
        // check that a notif is well received
        NotificationsTrayPage tray = assertMentionNotification();
        MentionNotificationPage mentionNotificationPage = new MentionNotificationPage(tray);
        mentionNotificationPage.openGroup(0);
        assertEquals("mentioned you on a comment on page Mention Comment Test Page",
            mentionNotificationPage.getText(0, 0));
        assertEquals("U3", mentionNotificationPage.getEmitter(0, 0));
        assertTrue(mentionNotificationPage.hasSummary(0, 0));
        assertEquals("<strong>Quote</strong> @U4 XYZ", mentionNotificationPage.getSummary(0, 0));
    }

    /**
     * Custom filters restrict the mentions of each channel. U5 receives the mentions by alert and in a daily email,
     * and ignores the mentions on c (and children) in alerts and on d (and children) in emails. U1 then mentions U5 on
     * each of the nested pages a/b/c/d/e, located under the test page.
     *
     * @param setup The test setup.
     * @param testReference The test page reference, under which the pages of the test are created.
     * @param testConfiguration The test configuration.
     * @throws Exception In case of error.
     */
    @Test
    @Order(3)
    void exclusiveFiltersOnMentions(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        List<DocumentReference> pages = List.of(
            getFilterPage(testReference),
            getFilterPage(testReference, "b"),
            getFilterPage(testReference, "b", "c"),
            getFilterPage(testReference, "b", "c", "d"),
            getFilterPage(testReference, "b", "c", "d", "e"));
        try {
            startMail(setup, testConfiguration);
            // The pages must exist to be selected in the location tree of the filters.
            for (DocumentReference page : pages) {
                setup.rest().runAs(U1_CREDENTIALS, rest -> rest.savePage(page, "Content", getTitle(page)));
            }
            setup.createUser(U5_USERNAME, USERS_PWD, "", "email", U5_EMAIL);

            setup.login(U5_USERNAME, USERS_PWD);
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(U5_USERNAME);
            setMentionsOnly(p);
            addMentionsFilter(p, NotificationFormat.ALERT, pages.get(2));
            addMentionsFilter(p, NotificationFormat.EMAIL, pages.get(3));

            // Mention from e to a: the mentions are processed in order, so once the mentions on a and b (the last
            // ones) are notified, the mentions on c, d and e have been processed too, and the filters are the only
            // reason why they are not listed.
            for (int i = pages.size() - 1; i >= 0; i--) {
                DocumentReference page = pages.get(i);
                setup.rest().runAs(U1_CREDENTIALS, rest -> rest.savePage(page, String.format(
                    "{{mention reference=\"xwiki:XWiki.%s\" style=\"LOGIN\" anchor=\"filter-mention-%s\"/}}",
                    U5_USERNAME, getTitle(page)), getTitle(page)));
            }

            // The notification menu lists only the mentions on a and b.
            setup.gotoPage(pages.get(0));
            waitOnNotificationCount("xwiki:XWiki." + U5_USERNAME, MAIN_WIKI, 2);
            assertEquals(Set.of("a", "b"), getMentionedPagesInTray());

            setup.loginAsSuperAdmin();
            SchedulerHomePage.gotoPage().clickJobActionTrigger(DAILY_EMAIL_JOB);

            // The daily email lists only the mentions on a, b and c.
            List<Email> emails = waitForEmails(U5_EMAIL);
            assertEquals(1, emails.size());
            assertEquals(Set.of("a", "b", "c"), getMentionedPages(emails.get(0)));
        } finally {
            setup.loginAsSuperAdmin();
            setup.deletePage(pages.get(0), true);
            setup.deletePage(USERS_SPACE, U5_USERNAME);
            stopMail();
        }
    }

    /**
     * A local user of a subwiki is mentioned on a page of the subwiki: the mention is notified in the notification menu
     * and in the daily email, sent with the mail configuration of the main wiki.
     *
     * @param wiki The subwiki.
     * @param setup The test setup.
     * @param testReference The test page reference, under which the pages of the test are created.
     * @param testConfiguration The test configuration.
     * @throws Exception In case of error.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false, extensions = {
        "org.xwiki.platform:xwiki-platform-mentions-ui",
        "org.xwiki.platform:xwiki-platform-scheduler-ui"
    })
    @Order(4)
    void mentionLocalSubwikiUser(WikiReference wiki, TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        DocumentReference page = testReference.replaceParent(testReference.getWikiReference(), wiki);
        try {
            // The mail is configured on the main wiki only.
            startMail(setup, testConfiguration);

            // Allow local users on the subwiki and create one.
            setup.setCurrentWiki(wiki.getName());
            setup.loginAsSuperAdmin();
            setup.updateObject("WikiManager", "WikiUserConfiguration", "WikiManager.WikiUserClass", 0, "userScope",
                "local_and_global");
            setup.createUser(LOCAL_USERNAME, USERS_PWD, "", "email", LOCAL_EMAIL);

            setup.login(LOCAL_USERNAME, USERS_PWD);
            setMentionsOnly(NotificationsUserProfilePage.gotoPage(LOCAL_USERNAME));

            // A global user mentions the local user on a page of the subwiki.
            setup.rest().runAs(U1_CREDENTIALS, rest -> rest.savePage(page, String.format(
                "{{mention reference=\"%s:XWiki.%s\" style=\"LOGIN\" anchor=\"subwiki-mention\"/}}", wiki.getName(),
                LOCAL_USERNAME), "Subwiki mention"));

            // The REST API is served by the main wiki, where the local user can't authenticate: wait as an
            // administrator, who is allowed to count the notifications of any user.
            setup.loginAsSuperAdmin();
            waitOnNotificationCount(String.format("%s:XWiki.%s", wiki.getName(), LOCAL_USERNAME), wiki.getName(), 1);
            setup.login(LOCAL_USERNAME, USERS_PWD);
            setup.gotoPage(page);
            assertEquals(Set.of("Subwiki mention"), getMentionedPagesInTray());

            setup.loginAsSuperAdmin();
            SchedulerHomePage.gotoPage().clickJobActionTrigger(DAILY_EMAIL_JOB);

            List<Email> emails = waitForEmails(LOCAL_EMAIL);
            assertEquals(1, emails.size());
            assertEquals(ADMIN_EMAIL, emails.get(0).getFromRecipient().getAddress());
            assertEquals(Set.of("Subwiki mention"), getMentionedPages(emails.get(0)));
        } finally {
            setup.setCurrentWiki(wiki.getName());
            setup.loginAsSuperAdmin();
            setup.deletePage(page);
            setup.deletePage(USERS_SPACE, LOCAL_USERNAME);
            setup.setCurrentWiki(MAIN_WIKI);
            stopMail();
        }
    }

    private String getTitle(DocumentReference page)
    {
        return page.getLastSpaceReference().getName();
    }

    private DocumentReference getFilterPage(TestReference testReference, String... children)
    {
        SpaceReference space = new SpaceReference("a", testReference.getLastSpaceReference());
        for (String child : children) {
            space = new SpaceReference(child, space);
        }
        return new DocumentReference("WebHome", space);
    }

    /**
     * Receive the mentions, and only them, both in the notification menu and by email.
     */
    private void setMentionsOnly(NotificationsUserProfilePage page) throws Exception
    {
        for (String format : List.of(ALERT_FORMAT, EMAIL_FORMAT)) {
            page.setApplicationState(PAGES_APPLICATION, format, BootstrapSwitch.State.OFF);
            page.setApplicationState(MENTIONS_APPLICATION, format, BootstrapSwitch.State.ON);
        }
    }

    /**
     * Ignore the mentions on the given page (and its children), for one channel.
     */
    private void addMentionsFilter(NotificationsUserProfilePage page, NotificationFormat format,
        DocumentReference location)
    {
        String[] path = Stream.concat(location.getSpaceReferences().stream().map(EntityReference::getName),
            Stream.of(location.getName())).toArray(String[]::new);
        CustomNotificationFilterModal modal = page.clickAddCustomFilter();
        modal.selectAction(FilterAction.IGNORE_EVENT);
        DocumentTreeElement locations = modal.getLocations();
        // The modal keeps the location selected for the previous filter.
        locations.clearSelection();
        locations.openToDocument(path).getDocumentNode(path).select();
        modal.getEventsSelector().selectByValue(MENTION_EVENT_TYPE);
        modal.selectFormats(Set.of(format));
        modal.clickSubmit();
    }

    /**
     * @return the titles of the pages of the mentions listed in the notification menu of the current user
     */
    private Set<String> getMentionedPagesInTray()
    {
        NotificationsTrayPage tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        MentionNotificationPage mentions = new MentionNotificationPage(tray);
        Set<String> pages = new HashSet<>();
        for (int i = 0; i < tray.getNotificationsListCount(); i++) {
            assertEquals(MENTION_EVENT_TYPE, tray.getNotificationType(i));
            mentions.openGroup(i);
            for (int j = 0; j < mentions.getNumberOfElements(i); j++) {
                String text = mentions.getText(i, j);
                assertThat(text, startsWith(MENTION_ON_PAGE));
                pages.add(text.substring(MENTION_ON_PAGE.length()));
            }
        }
        return pages;
    }

    private void startMail(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0,
            "host", testConfiguration.getServletEngine().getHostIP(),
            "port", "3025",
            "sendWaitTime", "0",
            "from", ADMIN_EMAIL);

        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    private void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
            this.mail = null;
        }
    }

    /**
     * Wait for the emails received by the given recipient.
     */
    private List<Email> waitForEmails(String recipient)
    {
        // Wait 30s at most, to make sure the emails have enough time to arrive, even if the CI is slow.
        long timeout = System.currentTimeMillis() + 30000L;
        List<Email> emails = getEmails(recipient);
        while (emails.isEmpty() && System.currentTimeMillis() < timeout) {
            this.mail.waitForIncomingEmail(1000L, this.mail.getReceivedMessages().length + 1);
            emails = getEmails(recipient);
        }
        assertFalse(emails.isEmpty(),
            String.format("Timeout reached while waiting for the emails of [%s].", recipient));
        return emails;
    }

    private List<Email> getEmails(String recipient)
    {
        return Stream.of(this.mail.getReceivedMessages())
            .map(EmailConverter::mimeMessageToEmail)
            .filter(email -> email.getRecipients().stream().map(Recipient::getAddress).anyMatch(recipient::equals))
            .toList();
    }

    /**
     * @return the titles of the pages of the mentions listed in the text version of the given email
     */
    private Set<String> getMentionedPages(Email email)
    {
        Set<String> pages = new HashSet<>();
        Matcher matcher = PLAIN_TEXT_EVENT.matcher(email.getPlainText());
        while (matcher.find()) {
            assertEquals(MENTION_EVENT_TYPE, matcher.group(1));
            pages.add(matcher.group(2));
        }
        return pages;
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
