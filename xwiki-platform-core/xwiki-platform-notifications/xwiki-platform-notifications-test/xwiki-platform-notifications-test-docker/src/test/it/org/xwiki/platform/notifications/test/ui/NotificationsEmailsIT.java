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
package org.xwiki.platform.notifications.test.ui;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.converter.EmailConverter;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.like.test.po.LikeButton;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage.EmailInterval;
import org.xwiki.platform.notifications.test.po.NotificationWatchButtonElement;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.NotificationsWatchModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterModal.NotificationFormat;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference.FilterAction;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the notifications emails.
 *
 * @version $Id$
 * @since 12.3RC1
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml,notification-filter-preferences.hbm.xml",
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.scheduler.SchedulerPlugin",
        // Switch to domain-based URL
        "xwikiCfgVirtualUsepath=0",
        // Send the live notification emails as soon as the events are processed, instead of waiting 10 minutes.
        "xwikiPropertiesAdditionalProperties=notifications.emails.live.graceTime=0"
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
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
class NotificationsEmailsIT
{
    private static final String FIRST_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "user1";

    private static final String SECOND_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "user2";

    private static final String FIRST_USER_PASSWORD = "notificationsUser1";

    private static final String SECOND_USER_PASSWORD = "notificationsUser2";

    private static final String SYSTEM = "org.xwiki.platform";

    private static final String NOTIFICATIONS_EMAIL_TEST = "NotificationsEmailTest";

    private static final String EMAIL_FORMAT = "email";

    private static final String ALERT_FORMAT = "alert";

    private static final String CREATE = "create";

    private static final String UPDATE = "update";

    private static final String MENTIONS = "mentions.application.name";

    private static final String LIKES = "like.application.name";

    private static final String MAIN_WIKI = "xwiki";

    private static final String USERS_SPACE = "XWiki";

    private static final String FILTERS_SPACE = "NotificationsEmailFilters";

    private static final String FILTER_AUTHOR_NAME = NotificationsEmailsIT.class.getSimpleName() + "FilterAuthor";

    private static final String EXCLUSIVE_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "Exclusive";

    private static final String INCLUSIVE_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "Inclusive";

    private static final String LIVE_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "Live";

    private static final String SUBWIKI_USER_NAME = NotificationsEmailsIT.class.getSimpleName() + "SubwikiUser";

    private static final String SUBWIKI_AUTHOR_NAME = NotificationsEmailsIT.class.getSimpleName() + "SubwikiAuthor";

    private static final String PASSWORD = "notificationsPassword";

    // Matches the first line of an event in the plain text part of a notification email, e.g.
    // "  [update] [Some title](https://...)." and captures the event type and the document title.
    private static final Pattern PLAIN_TEXT_EVENT = Pattern.compile("^\\s*\\[([^\\]]+)\\] \\[([^\\]]*)\\]\\(",
        Pattern.MULTILINE);

    public static final String USER_EMAIL = "test@xwiki.org";

    public static final String ADMIN_EMAIL = "admin@xwiki.org";

    private GreenMail mail;

    @Test
    void notificationsEmails(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        try {
            intialize(setup, testConfiguration);

            setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
            NotificationsUserProfilePage p;
            p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
            // Receive every event type by email, so that the text version of each of them can be checked.
            // We also enable the alert format to be able to wait on those notifications.
            for (String application : List.of(SYSTEM, MENTIONS, LIKES)) {
                p.setApplicationState(application, EMAIL_FORMAT, BootstrapSwitch.State.ON);
                p.setApplicationState(application, ALERT_FORMAT, BootstrapSwitch.State.ON);
            }

            // Start watching the wiki so that we receive notifications
            NotificationWatchButtonElement watchButtonElement = new NotificationWatchButtonElement();
            assertTrue(watchButtonElement.isNotSet());
            // And he will watch the entire wiki.
            NotificationsWatchModal notificationsWatchModal = watchButtonElement.openModal();
            assertEquals(List.of(
                NotificationsWatchModal.WatchOptions.WATCH_PAGE,
                NotificationsWatchModal.WatchOptions.WATCH_WIKI
            ), notificationsWatchModal.getAvailableOptions());
            notificationsWatchModal.selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);

            setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
            DocumentReference page1 = new DocumentReference("xwiki", NOTIFICATIONS_EMAIL_TEST, "Page1");
            DocumentReference page2 = new DocumentReference("xwiki", NOTIFICATIONS_EMAIL_TEST, "Page2");

            setup.createPage(page1, "Content 1", "Title 1");
            setup.createPage(page2, "Content 2", "Title 2");

            // Generate the other event types: update, comment, delete, mention and like.
            DocumentReference updatedPage = new DocumentReference(MAIN_WIKI, NOTIFICATIONS_EMAIL_TEST, "Updated");
            DocumentReference commentedPage = new DocumentReference(MAIN_WIKI, NOTIFICATIONS_EMAIL_TEST, "Commented");
            DocumentReference deletedPage = new DocumentReference(MAIN_WIKI, NOTIFICATIONS_EMAIL_TEST, "Deleted");
            DocumentReference mentionPage = new DocumentReference(MAIN_WIKI, NOTIFICATIONS_EMAIL_TEST, "Mention");
            setup.rest().savePage(updatedPage, "Content", "Updated title");
            setup.rest().savePage(updatedPage, "Updated content", "Updated title");
            setup.rest().savePage(commentedPage, "Content", "Commented title");
            setup.rest().addObject(commentedPage, "XWiki.XWikiComments", "author", "XWiki." + FIRST_USER_NAME,
                "comment", "A comment");
            setup.rest().savePage(deletedPage, "Content", "Deleted title");
            setup.rest().delete(deletedPage);
            setup.rest().savePage(mentionPage, String.format("Hello {{mention reference=\"xwiki:XWiki.%s\" "
                + "style=\"LOGIN\" anchor=\"email-mention\"/}}", SECOND_USER_NAME), "Mention title");
            setup.gotoPage(page2);
            new LikeButton().clickToLike();

            // Wait for the notifications to be handled.
            setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
            setup.gotoPage(page1);
            NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 11);

            // Trigger the notification email job
            setup.loginAsSuperAdmin();
            SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
            schedulerHomePage.clickJobActionTrigger("Notifications daily email");

            // Wait 30s instead of the default 5s to make sure the mail has enough time to arrive, even if the CI is slow.
            assertTrue(this.mail.waitForIncomingEmail(30000L, 1), "Timeout reached for getting notification email.");

            assertEquals(1, this.mail.getReceivedMessages().length);
            MimeMessage message = this.mail.getReceivedMessages()[0];

            // Convert to org.simplejavamail.email because it is more simple to read
            Email email = EmailConverter.mimeMessageToEmail(message);
            assertTrue(email.getSubject().endsWith("event(s) on the wiki"));
            assertEquals(ADMIN_EMAIL, email.getFromRecipient().getAddress());
            assertEquals(1, email.getRecipients().size());
            assertEquals(USER_EMAIL, email.getRecipients().get(0).getAddress());

            assertNotNull(email.getPlainText());
            assertNotNull(email.getHTMLText());
            assertNotNull(email.getAttachments());
            assertFalse(email.getAttachments().isEmpty());

            // Events inside an email comes in random order, so we just verify that all the expected content is there
            String plainTextContent = prepareMail(email.getPlainText());

            String expectedContent;
            expectedContent =
                prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedPlainTextMail1.txt")));
            assertTrue(plainTextContent.contains(expectedContent),
                String.format("Email is supposed to contain: [\n%s\n], but all we have is [\n%s\n].",
                    expectedContent, plainTextContent));
            expectedContent =
                prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedPlainTextMail2.txt")));
            assertTrue(plainTextContent.contains(expectedContent),
                String.format("Email is supposed to contain: [\n%s\n], but all we we have is [\n%s\n].",
                    expectedContent, plainTextContent));

            // Every other event type is listed in the text version, with the label of its type.
            Set<String> plainTextEvents = getPlainTextEvents(email);
            Set<String> expectedEvents = Set.of(
                "[update] Updated title",
                "[addComment] Commented title",
                "[delete] Deleted",
                "[mentions.mention] Mention title",
                "[org.xwiki.like.events.LikeRecordableEvent] Title 2");
            assertTrue(plainTextEvents.containsAll(expectedEvents),
                String.format("Expected the events %s in the text version but got %s.", expectedEvents,
                    plainTextEvents));

            // We also check the html content, this time using Pattern to allow performing the checks while ignoring some
            // elements such as random ids
            String htmlTextContent = prepareMail(email.getHTMLText());

            expectedContent = IOUtils.toString(getClass().getResourceAsStream("/expectedHtmlMail1.txt"),
                StandardCharsets.UTF_8);
            // We escape everything and we pay attention to ignore whitespaces on each lines
            expectedContent = Arrays.stream(expectedContent.split("\n"))
                .map(str -> String.format("\\Q%s\\E\\s*", str.trim()))
                .collect(Collectors.joining());

            Pattern pattern = Pattern.compile(expectedContent, Pattern.COMMENTS);
            assertTrue(pattern.matcher(htmlTextContent).find(), String.format("Email is supposed to contain: [\n%s\n], "
                + "but all we have is [\n%s\n].", expectedContent, htmlTextContent));

            expectedContent = IOUtils.toString(getClass().getResourceAsStream("/expectedHtmlMail2.txt"),
                StandardCharsets.UTF_8);
            // We escape everything and we pay attention to ignore whitespaces on each lines
            expectedContent = Arrays.stream(expectedContent.split("\n"))
                .map(str -> String.format("\\Q%s\\E\\s*", str.trim()))
                .collect(Collectors.joining());

            pattern = Pattern.compile(expectedContent, Pattern.COMMENTS);
            assertTrue(pattern.matcher(htmlTextContent).find(), String.format("Email is supposed to contain: [\n%s\n], "
                + "but all we have is [\n%s\n].", expectedContent, htmlTextContent));

            setup.rest().delete(page1);
            setup.rest().delete(page2);
            setup.rest().delete(updatedPage);
            setup.rest().delete(commentedPage);
            setup.rest().delete(mentionPage);
        } finally {
            cleanup(setup, FIRST_USER_NAME, SECOND_USER_NAME);
        }
    }

    /**
     * Custom filters restrict the events of each channel: the notification menu (alert) and the emails, both daily and
     * live. Three users watch the same updates of the nested pages a/b/c/d/e:
     * <ul>
     *     <li>the "exclusive" user follows the entire wiki, ignores the updates of c (and children) in alerts and of d
     *     (and children) in emails, and gets a daily email;</li>
     *     <li>the "inclusive" user doesn't follow the wiki, is notified of the updates of b (and children) in alerts
     *     and of c (and children) in emails, and gets a daily email;</li>
     *     <li>the "live" user follows the entire wiki, ignores the updates of c (and children) in emails, and gets an
     *     email for each event.</li>
     * </ul>
     */
    @Test
    void customFiltersOnAlertsAndEmails(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        try {
            configureMail(setup, testConfiguration);

            // Create the nested pages a/b/c/d/e before the users, so that their creation is not notified.
            List<DocumentReference> pages = List.of(
                getFilterPage(),
                getFilterPage("b"),
                getFilterPage("b", "c"),
                getFilterPage("b", "c", "d"),
                getFilterPage("b", "c", "d", "e"));
            for (DocumentReference page : pages) {
                setup.rest().savePage(page, "Content", getFilterPageTitle(page));
            }

            setup.createUser(FILTER_AUTHOR_NAME, PASSWORD, "");
            setup.createUser(EXCLUSIVE_USER_NAME, PASSWORD, "", "email", "exclusive@xwiki.org");
            setup.createUser(INCLUSIVE_USER_NAME, PASSWORD, "", "email", "inclusive@xwiki.org");
            setup.createUser(LIVE_USER_NAME, PASSWORD, "", "email", "live@xwiki.org");

            NotificationsUserProfilePage p = setUpEmailNotifications(setup, EXCLUSIVE_USER_NAME, EmailInterval.DAILY);
            addUpdateFilter(p, FilterAction.IGNORE_EVENT, NotificationFormat.ALERT, "b", "c");
            addUpdateFilter(p, FilterAction.IGNORE_EVENT, NotificationFormat.EMAIL, "b", "c", "d");

            p = setUpEmailNotifications(setup, INCLUSIVE_USER_NAME, EmailInterval.DAILY);
            addUpdateFilter(p, FilterAction.NOTIFY_EVENT, NotificationFormat.ALERT, "b");
            addUpdateFilter(p, FilterAction.NOTIFY_EVENT, NotificationFormat.EMAIL, "b", "c");
            // The wiki is not followed by default.
            assertTrue(new NotificationWatchButtonElement().isNotSet());

            p = setUpEmailNotifications(setup, LIVE_USER_NAME, EmailInterval.LIVE);
            addUpdateFilter(p, FilterAction.IGNORE_EVENT, NotificationFormat.EMAIL, "b", "c");

            // Follow the entire wiki only once all the preferences are saved: they are saved in the user profiles, and
            // these updates would otherwise be notified to the users following the wiki.
            for (String userName : List.of(EXCLUSIVE_USER_NAME, LIVE_USER_NAME)) {
                setup.login(userName, PASSWORD);
                NotificationsUserProfilePage.gotoPage(userName);
                followEntireWiki();
            }

            // Update the pages, from e to a: the live emails are sent in the order of the events, so once the live
            // emails for a and b are received, no live email can be pending for c, d or e.
            setup.login(FILTER_AUTHOR_NAME, PASSWORD);
            for (int i = pages.size() - 1; i >= 0; i--) {
                setup.rest().savePage(pages.get(i), "Updated content", getFilterPageTitle(pages.get(i)));
            }

            // The notification menu of the exclusive user lists only the updates of a and b.
            setup.login(EXCLUSIVE_USER_NAME, PASSWORD);
            setup.gotoPage(pages.get(0));
            NotificationsTrayPage.waitOnNotificationCount(getMainWikiUser(EXCLUSIVE_USER_NAME), MAIN_WIKI, 2);
            assertEquals(Set.of("a", "b"), getUpdatedPagesInTray());

            // The notification menu of the inclusive user lists only the updates of b and its children.
            setup.login(INCLUSIVE_USER_NAME, PASSWORD);
            setup.gotoPage(pages.get(0));
            NotificationsTrayPage.waitOnNotificationCount(getMainWikiUser(INCLUSIVE_USER_NAME), MAIN_WIKI, 4);
            assertEquals(Set.of("b", "c", "d", "e"), getUpdatedPagesInTray());

            // Wait for the live emails of the updates of a and b.
            waitForEmails("live@xwiki.org",
                emails -> getUpdatedPages(emails).containsAll(Set.of("a", "b")));

            setup.loginAsSuperAdmin();
            SchedulerHomePage.gotoPage().clickJobActionTrigger("Notifications daily email");

            List<Email> exclusiveEmails = waitForEmails("exclusive@xwiki.org", emails -> !emails.isEmpty());
            assertEquals(1, exclusiveEmails.size());
            assertEquals(Set.of("a", "b", "c"), getUpdatedPages(exclusiveEmails));

            List<Email> inclusiveEmails = waitForEmails("inclusive@xwiki.org", emails -> !emails.isEmpty());
            assertEquals(1, inclusiveEmails.size());
            assertEquals(Set.of("c", "d", "e"), getUpdatedPages(inclusiveEmails));

            // Check the live emails last, to leave the most time to any unexpected live email to be received. Events
            // processed together can be sent in the same live email, so the number of emails is not checked.
            assertEquals(Set.of("a", "b"), getUpdatedPages(getEmails("live@xwiki.org")));

            setup.deletePage(pages.get(0), true);
        } finally {
            setup.loginAsSuperAdmin();
            cleanup(setup, FILTER_AUTHOR_NAME, EXCLUSIVE_USER_NAME, INCLUSIVE_USER_NAME, LIVE_USER_NAME);
        }
    }

    /**
     * A local user of a subwiki receives live emails for the events of the subwiki, sent with the mail configuration
     * of the main wiki.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false, extensions = {
        "org.xwiki.platform:xwiki-platform-notifications-ui",
        "org.xwiki.platform:xwiki-platform-mentions-ui"
    })
    void liveEmailsOnSubwiki(WikiReference wiki, TestUtils setup, TestConfiguration testConfiguration)
        throws Exception
    {
        String subwikiUserEmail = "subwiki@xwiki.org";
        try {
            // The mail is configured on the main wiki only.
            configureMail(setup, testConfiguration);
            setup.createUser(SUBWIKI_AUTHOR_NAME, PASSWORD, "");

            // Allow local users on the subwiki and create one.
            setup.setCurrentWiki(wiki.getName());
            setup.loginAsSuperAdmin();
            setup.updateObject("WikiManager", "WikiUserConfiguration", "WikiManager.WikiUserClass", 0, "userScope",
                "local_and_global");
            setup.createUser(SUBWIKI_USER_NAME, PASSWORD, "", "email", subwikiUserEmail);
            // The email frequency selector saves through a URL using the domain of the subwiki, which the browser
            // can't reach since this test class switches to domain-based URLs. Set the frequency with REST instead.
            setup.rest().addObject(new DocumentReference(wiki.getName(), USERS_SPACE, SUBWIKI_USER_NAME),
                "XWiki.Notifications.Code.NotificationEmailPreferenceClass", "interval", "live");

            setup.login(SUBWIKI_USER_NAME, PASSWORD);
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(SUBWIKI_USER_NAME);
            assertEquals(EmailInterval.LIVE, p.getNotificationEmailInterval());
            // All the email toggles are on by default.
            for (String application : List.of(SYSTEM, MENTIONS)) {
                assertEquals(BootstrapSwitch.State.ON, p.getApplicationState(application, EMAIL_FORMAT));
            }
            followEntireWiki();

            // A global user creates, updates and deletes a page, comments another page and mentions the local user.
            setup.setCurrentWiki(MAIN_WIKI);
            setup.login(SUBWIKI_AUTHOR_NAME, PASSWORD);
            DocumentReference page = new DocumentReference(wiki.getName(), FILTERS_SPACE, "Page");
            DocumentReference commentedPage = new DocumentReference(wiki.getName(), FILTERS_SPACE, "Commented");
            DocumentReference mentionPage = new DocumentReference(wiki.getName(), FILTERS_SPACE, "Mention");
            setup.rest().savePage(page, "Content", "Page title");
            setup.rest().savePage(page, "Updated content", "Page title");
            setup.rest().delete(page);
            setup.rest().savePage(commentedPage, "Content", "Commented title");
            setup.rest().addObject(commentedPage, "XWiki.XWikiComments", "author",
                getMainWikiUser(SUBWIKI_AUTHOR_NAME), "comment", "A comment");
            setup.rest().savePage(mentionPage, String.format("Hello {{mention reference=\"%s:XWiki.%s\" "
                + "style=\"LOGIN\" anchor=\"subwiki-mention\"/}}", wiki.getName(), SUBWIKI_USER_NAME),
                "Mention title");

            Set<String> expectedEvents = Set.of(
                "[create] Commented title",
                "[addComment] Commented title",
                "[create] Mention title",
                "[mentions.mention] Mention title");
            // An email can be rendered after the page is deleted, and then displays the page name instead of its
            // title, so only check the types of the events of the deleted page.
            Set<String> expectedDeletedPageEventTypes = Set.of("[create]", "[update]", "[delete]");
            List<Email> emails = waitForEmails(subwikiUserEmail, received -> {
                Set<String> events = getEvents(received);
                Set<String> deletedPageEventTypes = events.stream()
                    .filter(event -> event.endsWith(" Page title") || event.endsWith(" Page"))
                    .map(event -> event.substring(0, event.indexOf(' ')))
                    .collect(Collectors.toSet());
                return events.containsAll(expectedEvents)
                    && deletedPageEventTypes.containsAll(expectedDeletedPageEventTypes);
            });
            for (Email email : emails) {
                assertEquals(ADMIN_EMAIL, email.getFromRecipient().getAddress());
            }

            setup.loginAsSuperAdmin();
            setup.deletePage(commentedPage);
            setup.deletePage(mentionPage);
        } finally {
            setup.setCurrentWiki(wiki.getName());
            setup.loginAsSuperAdmin();
            setup.deletePage(USERS_SPACE, SUBWIKI_USER_NAME);
            setup.setCurrentWiki(MAIN_WIKI);
            cleanup(setup, SUBWIKI_AUTHOR_NAME);
        }
    }

    private DocumentReference getFilterPage(String... children)
    {
        List<String> spaces = Stream.concat(Stream.of(FILTERS_SPACE, "a"), Stream.of(children)).toList();
        return new DocumentReference(MAIN_WIKI, spaces, "WebHome");
    }

    private String getFilterPageTitle(DocumentReference page)
    {
        return page.getLastSpaceReference().getName();
    }

    private String getMainWikiUser(String userName)
    {
        return String.format("%s:%s.%s", MAIN_WIKI, USERS_SPACE, userName);
    }

    /**
     * Log in as the given user and turn on, for the Pages application, both the notification menu and the emails,
     * sent at the given interval.
     */
    private NotificationsUserProfilePage setUpEmailNotifications(TestUtils setup, String userName,
        EmailInterval interval) throws Exception
    {
        setup.login(userName, PASSWORD);
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(userName);
        p.setNotificationEmailInterval(interval);
        p.setApplicationState(SYSTEM, ALERT_FORMAT, BootstrapSwitch.State.ON);
        p.setApplicationState(SYSTEM, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        return p;
    }

    /**
     * Add a custom filter on the "A page is modified" events of the given page of the filters space (and its
     * children), for one channel.
     */
    private void addUpdateFilter(NotificationsUserProfilePage page, FilterAction action, NotificationFormat format,
        String... children)
    {
        String[] path = Stream.concat(Stream.of(FILTERS_SPACE, "a"), Stream.concat(Stream.of(children),
            Stream.of("WebHome"))).toArray(String[]::new);
        CustomNotificationFilterModal modal = page.clickAddCustomFilter();
        modal.selectAction(action);
        DocumentTreeElement locations = modal.getLocations();
        // The modal keeps the location selected for the previous filter.
        locations.clearSelection();
        locations.openToDocument(path).getDocumentNode(path).select();
        modal.getEventsSelector().selectByValue(UPDATE);
        modal.selectFormats(Set.of(format));
        modal.clickSubmit();
    }

    private void followEntireWiki()
    {
        new NotificationWatchButtonElement().openModal()
            .selectOptionAndSave(NotificationsWatchModal.WatchOptions.WATCH_WIKI);
    }

    /**
     * @return the titles of the pages of the update events listed in the notification menu of the current user
     */
    private Set<String> getUpdatedPagesInTray()
    {
        NotificationsTrayPage tray = new NotificationsTrayPage();
        tray.showNotificationTray();
        Set<String> pages = new HashSet<>();
        for (int i = 0; i < tray.getNotificationsListCount(); i++) {
            if (UPDATE.equals(tray.getNotificationType(i))) {
                pages.add(tray.getNotificationPage(i));
            }
        }
        return pages;
    }

    private List<Email> getEmails(String recipient)
    {
        return Stream.of(this.mail.getReceivedMessages())
            .map(EmailConverter::mimeMessageToEmail)
            .filter(email -> email.getRecipients().stream().map(Recipient::getAddress).anyMatch(recipient::equals))
            .toList();
    }

    /**
     * Wait until the emails received by the given recipient match the given condition.
     */
    private List<Email> waitForEmails(String recipient, Predicate<List<Email>> condition)
    {
        // Wait 30s at most, to make sure the emails have enough time to arrive, even if the CI is slow.
        long timeout = System.currentTimeMillis() + 30000L;
        List<Email> emails = getEmails(recipient);
        while (!condition.test(emails) && System.currentTimeMillis() < timeout) {
            this.mail.waitForIncomingEmail(1000L, this.mail.getReceivedMessages().length + 1);
            emails = getEmails(recipient);
        }
        assertTrue(condition.test(emails),
            String.format("Timeout reached while waiting for the emails of [%s]. Received events: %s", recipient,
                getEvents(emails)));
        return emails;
    }

    /**
     * @return the events listed in the text version of the given emails, as "[type] title"
     */
    private Set<String> getEvents(List<Email> emails)
    {
        Set<String> events = new HashSet<>();
        for (Email email : emails) {
            events.addAll(getPlainTextEvents(email));
        }
        return events;
    }

    private Set<String> getPlainTextEvents(Email email)
    {
        Set<String> events = new HashSet<>();
        Matcher matcher = PLAIN_TEXT_EVENT.matcher(email.getPlainText());
        while (matcher.find()) {
            events.add(String.format("[%s] %s", matcher.group(1), matcher.group(2)));
        }
        return events;
    }

    /**
     * @return the titles of the pages of the update events listed in the given emails
     */
    private Set<String> getUpdatedPages(List<Email> emails)
    {
        String prefix = String.format("[%s] ", UPDATE);
        return getEvents(emails).stream()
            .filter(event -> event.startsWith(prefix))
            .map(event -> event.substring(prefix.length()))
            .collect(Collectors.toSet());
    }

    private String prepareMail(String email)
    {
        StringBuilder stringBuilder = new StringBuilder();
        // Some part of the email is unique (dates), so we remove them before comparing emails
        Scanner scanner = new Scanner(email);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith(String.format("  %d", Calendar.getInstance().get(Calendar.YEAR)))) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        }
        scanner.close();
        return stringBuilder.toString();
    }

    private void intialize(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Create the two users we will be using
        setup.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        setup.createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "email", USER_EMAIL);

        NotificationsUserProfilePage p;

        setup.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.disableAllParameters();
        // Enable own filter
        p.getSystemNotificationFilterPreferences().get(2).setEnabled(true);

        setup.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllParameters();

        configureMail(setup, testConfiguration);
    }

    private void configureMail(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0,
            "host", testConfiguration.getServletEngine().getHostIP(),
            "port", "3025",
            "sendWaitTime", "0",
            "from", ADMIN_EMAIL);

        // To ensure that this configuration is taken into account inside mail links.
        setup.setMainWikiDescriptorTarget("externaldomain", 4242, true);

        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    private void cleanup(TestUtils testUtils, String... userNames)
    {
        for (String userName : userNames) {
            testUtils.deletePage(USERS_SPACE, userName);
        }
        if (this.mail != null) {
            this.mail.stop();
        }
    }
}
