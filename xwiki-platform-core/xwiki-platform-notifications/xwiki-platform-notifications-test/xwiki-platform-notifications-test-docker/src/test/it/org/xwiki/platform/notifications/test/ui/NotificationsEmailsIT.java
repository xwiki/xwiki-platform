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

import java.util.Calendar;
import java.util.Scanner;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.Email;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
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
        "xwikiCfgVirtualUsepath=0"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-mail-send-storage",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-eventstream-store-hibernate",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr"
    }
)
public class NotificationsEmailsIT
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

    public static final String USER_EMAIL = "test@xwiki.org";
    public static final String ADMIN_EMAIL = "admin@xwiki.org";

    private GreenMail mail;

    @BeforeAll
    public void setup(TestUtils testUtils, TestConfiguration testConfiguration) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        // Create the two users we will be using
        testUtils.createUser(FIRST_USER_NAME, FIRST_USER_PASSWORD, "", "");
        testUtils.createUser(SECOND_USER_NAME, SECOND_USER_PASSWORD, "", "email", USER_EMAIL);

        NotificationsUserProfilePage p;

        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(FIRST_USER_NAME);
        p.disableAllParameters();
        // Enable own filter
        p.getNotificationFilterPreferences().get(2).setEnabled(true);

        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.disableAllParameters();

        testUtils.loginAsSuperAdmin();
        testUtils.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0,
            "host", testConfiguration.getServletEngine().getHostIP(),
            "port", "3025",
            "sendWaitTime", "0",
            "from", ADMIN_EMAIL);

        // To ensure that this configuration is taken into account inside mail links.
        testUtils.updateObject("XWiki", "XWikiServerXwiki", "XWiki.XWikiServerClass", 0, "server", "externaldomain",
            "port", "4242", "secure", "1");

        mail = new GreenMail(ServerSetupTest.SMTP);
        mail.start();
    }

    @AfterAll
    public void tearDown(TestUtils testUtils)
    {
        testUtils.deletePage("XWiki", FIRST_USER_NAME);
        testUtils.deletePage("XWiki", SECOND_USER_NAME);
        mail.stop();
    }

    @Test
    public void notificationsEmails(TestUtils testUtils) throws Exception
    {
        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        NotificationsUserProfilePage p;
        p = NotificationsUserProfilePage.gotoPage(SECOND_USER_NAME);
        p.getApplication(SYSTEM).setCollapsed(false);
        p.setEventTypeState(SYSTEM, CREATE, EMAIL_FORMAT, BootstrapSwitch.State.ON);

        // We also enable on alert format to be able to wait on those notifications.
        p.setEventTypeState(SYSTEM, CREATE, ALERT_FORMAT, BootstrapSwitch.State.ON);

        testUtils.login(FIRST_USER_NAME, FIRST_USER_PASSWORD);
        DocumentReference page1 = new DocumentReference("xwiki", NOTIFICATIONS_EMAIL_TEST, "Page1");
        DocumentReference page2 = new DocumentReference("xwiki", NOTIFICATIONS_EMAIL_TEST, "Page2");

        testUtils.createPage(NOTIFICATIONS_EMAIL_TEST, "Page1", "Content 1", "Title 1");
        testUtils.createPage(NOTIFICATIONS_EMAIL_TEST, "Page2", "Content 2", "Title 2");

        // Wait for the notifications to be handled.
        testUtils.login(SECOND_USER_NAME, SECOND_USER_PASSWORD);
        testUtils.gotoPage(page1);
        NotificationsTrayPage.waitOnNotificationCount("xwiki:XWiki." + SECOND_USER_NAME, "xwiki", 2);

        // Trigger the notification email job
        testUtils.loginAsSuperAdmin();
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.clickJobActionTrigger("Notifications daily email");

        // Wait 30s instead of the default 5s to make sure the mail has enough time to arrive, even if the CI is slow.
        assertTrue(mail.waitForIncomingEmail(30000L, 1), "Timeout reached for getting notification email.");

        assertEquals(1, mail.getReceivedMessages().length);
        MimeMessage message = mail.getReceivedMessages()[0];

        // Convert to org.simplejavamail.email because it is more simple to read
        Email email = EmailConverter.mimeMessageToEmail(message);
        assertTrue(email.getSubject().endsWith("event(s) on the wiki"));
        assertEquals(ADMIN_EMAIL, email.getFromRecipient().getAddress());
        assertEquals(1, email.getRecipients().size());
        assertEquals(USER_EMAIL, email.getRecipients().get(0).getAddress());

        assertNotNull(email.getText());
        assertNotNull(email.getTextHTML());
        assertNotNull(email.getAttachments());
        assertFalse(email.getAttachments().isEmpty());

        // Events inside an email comes in random order, so we just verify that all the expected content is there
        String plainTextContent = prepareMail(email.getText());
        String expectedContent;
        expectedContent = prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail1.txt")));
        assertTrue(plainTextContent.contains(expectedContent),
            String.format("Email is supposed to contain: [\n%s\n], but all we have is [\n%s\n].",
                expectedContent, plainTextContent));
        expectedContent = prepareMail(IOUtils.toString(getClass().getResourceAsStream("/expectedMail2.txt")));
        assertTrue(plainTextContent.contains(expectedContent),
            String.format("Email is supposed to contain: [\n%s\n], but all we we have is [\n%s\n].",
                expectedContent, plainTextContent));

        testUtils.rest().delete(page1);
        testUtils.rest().delete(page2);
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
}
