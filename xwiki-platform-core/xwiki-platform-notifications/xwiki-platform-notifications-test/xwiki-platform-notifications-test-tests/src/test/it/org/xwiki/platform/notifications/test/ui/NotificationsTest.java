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

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.Email;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.mail.test.po.SendMailAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.platform.notifications.test.po.preferences.ApplicationPreferences;
import org.xwiki.platform.notifications.test.po.preferences.filters.NotificationFilterPreference;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.BootstrapSwitch;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.user.test.po.ProfileEditPage;
import org.xwiki.user.test.po.ProfileUserProfilePage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Perform tests on the notifications module.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public class NotificationsTest extends AbstractTest
{
    private static final String FIRST_USER_NAME = "user1";
    private static final String SECOND_USER_NAME = "user2";
    private static final String SUPERADMIN_USER_NAME = "superadmin";

    private static final String FIRST_USER_PASSWORD = "notificationsUser1";
    private static final String SECOND_USER_PASSWORD = "notificationsUser2";
    private static final String SUPERADMIN_PASSWORD = "pass";

    // Number of pages that have to be created in order for the notifications badge to show «X+»
    private static final int PAGES_TOP_CREATION_COUNT = 21;

    private static final String SYSTEM = "org.xwiki.platform";

    private static final String ALERT_FORMAT = "alert";

    private static final String EMAIL_FORMAT = "email";

    private static final String ADD_COMMENT = "addComment";

    private static final String CREATE = "create";

    private static final String DELETE = "delete";

    private static final String UPDATE = "update";

    private static GreenMail mail;

    @Before
    public void setUpUsers() throws Exception
    {
        // Create the two users we will be using
        getUtil().createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        getUtil().createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "");

        NotificationsUserProfilePage p;

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.disableAllParameters();
        // Enable own filter
        p.getNotificationFilterPreferences().get(2).setEnabled(true);

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllParameters();
        ProfileEditPage profileEditPage = ProfileUserProfilePage.gotoPage(SECOND_USER_NAME).editProfile();
        profileEditPage.setUserEmail("test@xwiki.org");
        profileEditPage.clickSaveAndView(true);
    }

    @Before
    public void cleanPages() throws Exception
    {
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        getUtil().rest().deletePage(getTestClassName(), "NotificationDisplayerClassTest");
        getUtil().rest().deletePage(getTestClassName(), "ARandomPageThatShouldBeModified");
    }

    @Before
    public void setUpEmails() throws Exception
    {
        if (this.mail != null) {
            // Already done
            return;
        }

        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        AdministrationPage wikiAdministrationPage = AdministrationPage.gotoPage();
        wikiAdministrationPage.clickSection("Mail", "Mail Sending");
        SendMailAdministrationSectionPage sendMailPage = new SendMailAdministrationSectionPage();
        sendMailPage.setHost("localhost");
        sendMailPage.setPort(String.valueOf(ServerSetupTest.SMTP.getPort()));
        sendMailPage.setEmailAddressToSendFrom("test@xwiki.org");

        // Make sure we don't wait between email sending in order to speed up the test (and not incur timeouts when
        // we wait to receive the mails)
        sendMailPage.setSendWaitTime("0");
        sendMailPage.clickSave();

        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    @AfterClass
    public static void stopMail()
    {
        if (mail != null) {
            mail.stop();
            mail = null;
        }
    }

    @Test
    public void testNotificationsSwitches() throws Exception
    {
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

        assertEquals(1, p.getApplicationPreferences().size());

        // Open system
        ApplicationPreferences system = p.getApplication(SYSTEM);
        assertTrue(system.isCollapsed());
        system.setCollapsed(false);
        assertFalse(system.isCollapsed());
        assertEquals("Pages", system.getApplicationName());
        assertEquals("A comment is posted", p.getEventType(SYSTEM, ADD_COMMENT).getEventTypeDescription());
        assertEquals("A new page is created", p.getEventType(SYSTEM, CREATE).getEventTypeDescription());
        assertEquals("A page is deleted", p.getEventType(SYSTEM, DELETE).getEventTypeDescription());
        assertEquals("A page is modified", p.getEventType(SYSTEM, UPDATE).getEventTypeDescription());

        // Check default
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable alert on system
        p.setApplicationState(SYSTEM, ALERT_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.ON , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable email on system
        p.setApplicationState(SYSTEM, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.ON, p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable email on system
        p.setApplicationState(SYSTEM, EMAIL_FORMAT, BootstrapSwitch.State.OFF);
        assertEquals(BootstrapSwitch.State.ON , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Disable alert on "update"
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        assertEquals(BootstrapSwitch.State.UNDETERMINED , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Enable email on "delete"
        p.setEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT, BootstrapSwitch.State.ON);
        assertEquals(BootstrapSwitch.State.UNDETERMINED , p.getApplicationState(SYSTEM, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.UNDETERMINED, p.getApplicationState(SYSTEM, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, ADD_COMMENT, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, CREATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.ON , p.getEventTypeState(SYSTEM, DELETE, EMAIL_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT));
        assertEquals(BootstrapSwitch.State.OFF, p.getEventTypeState(SYSTEM, UPDATE, EMAIL_FORMAT));

        // Forget it
        p.disableAllParameters();
    }

    @Test
    public void testNotifications() throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;

        // The user 1 creates a new page, the user 2 shouldn’t receive any notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage(getTestClassName(), "WebHome", "Content from " + FIRST_USER_NAME, "Page title");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");

        tray = new NotificationsTrayPage();
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will now enable his notifications for new pages
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Yes we wait on a timer, but it is to be sure the following events will be stored AFTER the settings have been
        // changed.
        Thread.sleep(1000);

        // We create a lot of pages in order to test the notification badge
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        for (int i = 1; i < PAGES_TOP_CREATION_COUNT; i++) {
            getUtil().createPage(getTestClassName(), "Page" + i, "Simple content", "Simple title");
        }
        getUtil().createPage(getTestClassName(), "DTP", "Deletion test page", "Deletion test content");

        // Check that the badge is showing «20+»
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(Integer.MAX_VALUE, tray.getNotificationsCount());

        // Ensure that the notification list is displaying the correct amount of unread notifications
        // (max 10 notifications by default)
        assertEquals(10, tray.getUnreadNotificationsCount());
        assertEquals(0, tray.getReadNotificationsCount());
        tray.markAsRead(0);
        assertEquals(9, tray.getUnreadNotificationsCount());
        assertEquals(1, tray.getReadNotificationsCount());

        // Ensure that a notification has a correct type
        assertEquals("create", tray.getNotificationType(0));

        // Reset the notifications count of the user 2
        tray.clearAllNotifications();
        assertEquals(0, tray.getNotificationsCount());
        assertFalse(tray.areNotificationsAvailable());

        // The user 2 will get notifications only for pages deletions
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        p.setEventTypeState(SYSTEM, DELETE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Yes we wait on a timer, but it is to be sure the following events will be stored AFTER the settings have been
        // changed.
        Thread.sleep(1000);

        // Delete the "Deletion test page" and test the notification
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().deletePage(getTestClassName(), "DTP");

        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(1, tray.getNotificationsCount());
    }

    @Test
    public void testNotificationsEmails() throws Exception
    {
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        NotificationsUserProfilePage p;
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT, BootstrapSwitch.State.ON);

        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        DocumentReference page1 = new DocumentReference("xwiki", getTestClassName(), "Page1");
        DocumentReference page2 = new DocumentReference("xwiki", getTestClassName(), "Page2");

        // Yes we wait on a timer, but it is to be sure the following events will be stored AFTER the settings have been
        // changed.
        Thread.sleep(1000);

        getUtil().createPage(getTestClassName(), "Page1", "Content 1", "Title 1");
        getUtil().createPage(getTestClassName(), "Page2", "Content 2", "Title 2");

        // Trigger the notification email job
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.clickJobActionTrigger("Notifications daily email");
        this.mail.waitForIncomingEmail(1);

        assertEquals(1, this.mail.getReceivedMessages().length);
        MimeMessage message = this.mail.getReceivedMessages()[0];

        // Convert to org.simplejavamail.email because it is more simple to read
        Email email = EmailConverter.mimeMessageToEmail(message);
        assertTrue(email.getSubject().endsWith("event(s) on the wiki"));
        assertEquals("test@xwiki.org", email.getFromRecipient().getAddress());

        assertNotNull(email.getText());
        assertNotNull(email.getTextHTML());
        assertNotNull(email.getAttachments());
        assertFalse(email.getAttachments().isEmpty());

        // Events inside an email comes in random order, so we just verify that all the expected content is there
        String plainTextContent = prepareMail(email.getText());
        String expectedContent;
        expectedContent = prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail1.txt")));
        assertTrue(String.format("Email is supposed to contain: [\n%s\n], but all we have is [\n%s\n].",
                expectedContent, plainTextContent),
                plainTextContent.contains(expectedContent));
        expectedContent = prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail2.txt")));
        assertTrue(String.format("Email is supposed to contain: [\n%s\n], but all we we have is [\n%s\n].",
                expectedContent, plainTextContent),
                plainTextContent.contains(expectedContent));

        getUtil().rest().delete(page1);
        getUtil().rest().delete(page2);
    }

    private String prepareMail(String email) {
        StringBuilder stringBuilder = new StringBuilder();
        // Some part of the email is unique (dates), so we remove them before comparing emails
        Scanner scanner = new Scanner(email);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.startsWith(String.format("  %d", Calendar.getInstance().get(Calendar.YEAR))) &&
                    !line.startsWith("  2017/06/27")) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        }
        scanner.close();
        return stringBuilder.toString();
    }

    @Test
    public void testCompositeNotifications() throws Exception
    {
        NotificationsUserProfilePage p;
        NotificationsTrayPage tray;
        // Now we enable "create", "update" and "comment" for user 2
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.OFF);
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);
        p.setEventTypeState(SYSTEM, ADD_COMMENT, ALERT_FORMAT, BootstrapSwitch.State.ON);
        getUtil().gotoPage("Main", "WebHome");
        tray = new NotificationsTrayPage();
        tray.clearAllNotifications();

        // Create a page, edit it twice, and finally add a comment
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().createPage(getTestClassName(), "Linux", "Simple content", "Linux as a title");
        ViewPage page = getUtil().gotoPage(getTestClassName(), "Linux");
        page.edit();
        WikiEditPage edit = new WikiEditPage();
        edit.setContent("Linux is a part of GNU/Linux");
        edit.clickSaveAndView(true);
        page = new ViewPage();
        page.edit();
        edit = new WikiEditPage();
        edit.setContent("Linux is a part of GNU/Linux - it's the kernel");
        edit.clickSaveAndView(true);
        page = getUtil().gotoPage(getTestClassName(), "Linux");
        CommentsTab commentsTab = page.openCommentsDocExtraPane();
        commentsTab.postComment("Linux is a great OS", true);

        // Check that events have been grouped together (see: https://jira.xwiki.org/browse/XWIKI-14114)
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");
        tray = new NotificationsTrayPage();
        assertEquals(2, tray.getNotificationsCount());
        assertEquals("Linux as a title", tray.getNotificationPage(0));
        assertTrue(tray.getNotificationDescription(0).startsWith("commented by user1"));
        assertEquals("Linux as a title", tray.getNotificationPage(1));
        assertEquals("update", tray.getNotificationType(1));
        assertTrue(tray.getNotificationDescription(1).startsWith("edited by user1"));
        tray.clearAllNotifications();
    }

    @Test
    public void testNotificationDisplayerClass() throws Exception
    {
        // Create the pages and a custom displayer for "update" events
        getUtil().login(SUPERADMIN_USER_NAME, SUPERADMIN_PASSWORD);

        getUtil().gotoPage(getTestClassName(), "WebHome");
        getUtil().createPage(getTestClassName(), "ARandomPageThatShouldBeModified",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page");

        getUtil().createPage(getTestClassName(), "NotificationDisplayerClassTest",
                "Page used for the tests of the NotificationDisplayerClass XObject.", "Test page 2");

        Map<String, String> notificationDisplayerParameters = new HashMap<String, String>()  {{
            put("XWiki.Notifications.Code.NotificationDisplayerClass_0_eventType", "update");
            put("XWiki.Notifications.Code.NotificationDisplayerClass_0_notificationTemplate",
                    "This is a test template");
        }};

        ObjectEditPage editObjects = getUtil().editObjects(getTestClassName(), "NotificationDisplayerClassTest");
        editObjects.addObject("XWiki.Notifications.Code.NotificationDisplayerClass");
        editObjects.getObjectsOfClass("XWiki.Notifications.Code.NotificationDisplayerClass")
                .get(0).fillFieldsByName(notificationDisplayerParameters);
        editObjects.clickSaveAndContinue(true);

        // Login as first user, and enable notifications on document updates
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, UPDATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        // Login as second user and modify ARandomPageThatShouldBeModified
        getUtil().login(SECOND_USER_NAME, SECOND_USER_PASSWORD);

        ViewPage viewPage = getUtil().gotoPage(getTestClassName(), "ARandomPageThatShouldBeModified");
        viewPage.edit();
        WikiEditPage editPage = new WikiEditPage();
        editPage.setContent("Something");
        editPage.clickSaveAndView(true);

        // Login as the first user, ensure that the notification is displayed with a custom template
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        getUtil().gotoPage(getTestClassName(), "WebHome");

        NotificationsTrayPage tray = new NotificationsTrayPage();
        assertEquals("This is a test template", tray.getNotificationRawContent(0));
    }

    @Test
    public void testNotificationFiltersDefaultValues() throws Exception
    {
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        // Verify the default state of the filters
        NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
        assertEquals(6, preferences.size());

        // Filter 0
        assertEquals("Minor Event (Alert)", preferences.get(0).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(0).getFilterType());
        assertTrue(preferences.get(0).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert"), preferences.get(0).getFormats());
        assertTrue(preferences.get(0).isEnabled());

        // Filter 1
        assertEquals("Minor Event (Email)", preferences.get(1).getFilterName());
        assertEquals("Hide notifications concerning minor changes on pages", preferences.get(1).getFilterType());
        assertTrue(preferences.get(1).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Email"), preferences.get(1).getFormats());
        assertTrue(preferences.get(1).isEnabled());

        // Filter 2
        assertEquals("Own Events Filter", preferences.get(2).getFilterName());
        assertEquals("Hide notifications about your own activity", preferences.get(2).getFilterType());
        assertTrue(preferences.get(2).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(2).getFormats());
        assertTrue(preferences.get(2).isEnabled());

        // Filter 3
        assertEquals("Read Event Filter (Alert)", preferences.get(3).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(3).getFilterType());
        assertTrue(preferences.get(3).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert"), preferences.get(3).getFormats());
        assertFalse(preferences.get(3).isEnabled());

        // Filter 4
        assertEquals("Read Event Filter (Email)", preferences.get(4).getFilterName());
        assertEquals("Hide notifications that you have marked as read", preferences.get(4).getFilterType());
        assertTrue(preferences.get(4).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Email"), preferences.get(4).getFormats());
        assertFalse(preferences.get(4).isEnabled());

        // Filter 5
        assertEquals("System Filter", preferences.get(5).getFilterName());
        assertEquals("Hide notifications from the System user", preferences.get(5).getFilterType());
        assertTrue(preferences.get(5).getEventTypes().isEmpty());
        assertEquals(Arrays.asList("Alert", "Email"), preferences.get(5).getFormats());
        assertTrue(preferences.get(5).isEnabled());
    }

    @Test
    public void testOwnEventFilter() throws Exception
    {
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        try {
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();

            // Now let's do some changes (own even filter)
            p.setApplicationState(SYSTEM, "alert", BootstrapSwitch.State.ON);
            assertEquals("Own Events Filter", preferences.get(2).getFilterName());
            preferences.get(2).setEnabled(false);
            getUtil().createPage(getTestClassName(), getTestMethodName(), "", "");
            // Refresh that page
            getUtil().gotoPage(getTestClassName(), getTestMethodName());
            NotificationsTrayPage notificationsTrayPage = new NotificationsTrayPage();
            assertEquals(1, notificationsTrayPage.getNotificationsCount());
            assertEquals("created by user1\n" + "moments ago",
                    notificationsTrayPage.getNotificationDescription(0));
            assertEquals(getTestMethodName(), notificationsTrayPage.getNotificationPage(0));

            // Go back to enable the own even filter
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals("Own Events Filter", preferences.get(2).getFilterName());
            assertFalse(preferences.get(2).isEnabled());
            preferences.get(2).setEnabled(true);
            getUtil().gotoPage(getTestClassName(), getTestMethodName());
            notificationsTrayPage = new NotificationsTrayPage();
            assertEquals(0, notificationsTrayPage.getNotificationsCount());
        } finally {
            // Clean up
            getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        }
    }

    @Test
    public void testWatchedPages() throws Exception
    {
        getUtil().login(FIRST_USER_NAME, FIRST_USER_PASSWORD);

        try {
            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);

            // Enable the notifications
            p.setApplicationState(SYSTEM, "alert", BootstrapSwitch.State.ON);

            // Create a page
            getUtil().createPage(getTestClassName(), getTestMethodName(), "", "");
            NotificationsTrayPage trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();

            // Check if the page is watched
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertTrue(trayPage.isWikiWatched());

            // Unwatch the wiki
            trayPage.setWikiWatchedState(false);
            // Verify all other buttons are updated
            assertFalse(trayPage.isPageOnlyWatched());
            assertFalse(trayPage.arePageAndChildrenWatched());

            // Watch the space
            trayPage.setPageAndChildrenWatchedState(true);
            // Verify the other button is updated
            assertTrue(trayPage.isPageOnlyWatched());

            // Unwatch the page
            trayPage.setPageOnlyWatchedState(false);
            // Verify the whole status
            assertFalse(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertFalse(trayPage.isWikiWatched());

            // Go back to the preferences
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
            assertEquals(9, preferences.size());

            // Filter 6
            assertTrue(preferences.get(6).getFilterName().contains("Wiki"));
            assertEquals("", preferences.get(6).getLocation());
            assertEquals("Exclusive", preferences.get(6).getFilterType());
            assertTrue(preferences.get(6).getEventTypes().isEmpty());;
            assertTrue(preferences.get(6).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(6).isEnabled());

            // Filter 7
            assertTrue(preferences.get(7).getFilterName().contains("Page and children"));
            assertEquals(getTestClassName() + ".WebHome", preferences.get(7).getLocation());
            assertEquals("Inclusive", preferences.get(7).getFilterType());
            assertTrue(preferences.get(7).getEventTypes().isEmpty());
            assertTrue(preferences.get(7).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(7).isEnabled());

            // Filter 8
            assertTrue(preferences.get(8).getFilterName().contains("Page only"));
            assertEquals(getTestClassName() + "." + getTestMethodName(), preferences.get(8).getLocation());
            assertEquals("Exclusive", preferences.get(8).getFilterType());
            assertTrue(preferences.get(8).getEventTypes().isEmpty());
            assertTrue(preferences.get(8).getFormats().containsAll(Arrays.asList("Email", "Alert")));
            assertTrue(preferences.get(8).isEnabled());

            // Disable filter 6
            preferences.get(6).setEnabled(false);
            // Refresh the page
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            // Verify the change have been saved
            assertFalse(preferences.get(6).isEnabled());

            // Delete these new filters
            preferences.get(8).delete();
            // The livetable page is refreshed so we need to load back the filter preferences
            p.getNotificationFilterPreferences().get(7).delete();
            p.getNotificationFilterPreferences().get(6).delete();

            // Verify it's all like the beginning
            getUtil().gotoPage(getTestClassName(), getTestMethodName());
            trayPage = new NotificationsTrayPage();
            trayPage.showNotificationTray();
            assertTrue(trayPage.isPageOnlyWatched());
            assertTrue(trayPage.arePageAndChildrenWatched());
            assertTrue(trayPage.isWikiWatched());

            // Go back to the preferences
            p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            preferences = p.getNotificationFilterPreferences();
            assertEquals(6, preferences.size());

        } finally {
            // Clean up
            getUtil().rest().deletePage(getTestClassName(), getTestMethodName());

            NotificationsUserProfilePage p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
            List<NotificationFilterPreference> preferences = p.getNotificationFilterPreferences();
            while (preferences.size() > 6) {
                preferences.get(preferences.size() - 1).delete();
                // Reload teh livetable
                preferences = p.getNotificationFilterPreferences();
            }
        }
    }
}
