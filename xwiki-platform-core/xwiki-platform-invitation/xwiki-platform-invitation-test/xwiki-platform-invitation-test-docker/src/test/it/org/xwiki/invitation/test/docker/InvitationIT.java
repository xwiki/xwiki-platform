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
package org.xwiki.invitation.test.docker;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.invitation.test.po.InspectInvitationsPage;
import org.xwiki.invitation.test.po.InvitationActionConfirmationElement;
import org.xwiki.invitation.test.po.InvitationGuestActionsPage;
import org.xwiki.invitation.test.po.InvitationMessageDisplayElement;
import org.xwiki.invitation.test.po.InvitationSenderPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.RegistrationPage;
import org.xwiki.test.ui.po.TableElement;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests invitation application.
 *
 * @version $Id$
 * @since 14.2
 */
@UITest(
    sshPorts = {
        // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
        // by GreenMail running on the host.
        3025
    },
    properties = {
        // The Mail module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml",
        // Add the MailSender plugin used to send mails
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.mailsender.MailSenderPlugin",
        // The invitation app requires that InvitationGuestActions is saved with PR or a warning message will be
        // displayed and will fail acceptInvitationToClosedWiki
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Invitation\\.InvitationGuestActions"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-mail-send-storage",
        // XWiki needs the mailsender plugin JAR to be present before it starts since it's not an extension and it
        // cannot be provisioned after XWiki is started!
        "org.xwiki.platform:xwiki-platform-mailsender",
        // MailSender plugin uses MailSenderConfiguration from xwiki-platform-mail-api so we need to provide an
        // implementation for it.
        "org.xwiki.platform:xwiki-platform-mail-send-default"
    }
)
class InvitationIT
{
    private static boolean initialized;

    private InvitationSenderPage senderPage;

    private GreenMail greenMail;

    @BeforeEach
    void setUp(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // TODO: replace with @BeforeAll the parts of the setup that can be executed only once.
        // Login as admin and delete existing messages.
        setup.loginAsSuperAdmin();
        setup.recacheSecretToken();
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);
        setup.rest().deletePage("Invitation", "InvitationMessages");

        if (!initialized) {
            configureEmail(setup, testConfiguration);

            // We have to go to sender page before any config shows up.
            InvitationSenderPage.gotoPage();

            AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
            // Set port to 3025
            config.getFormContainerElement("admin-page-content")
                .setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_smtp_port"),
                    "3025");
            config.getFormContainerElement("admin-page-content")
                .setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_smtp_server"),
                    testConfiguration.getServletEngine().getHostIP());
            // Make sure that by default we don't allow non admin to send emails to multiple addresses
            config.getFormContainerElement().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "false");
            config.clickSave(true);

            // Make sure the users we're registering in testAcceptInvitation and testAcceptInvitationToCloseWiki don't
            // exist.
            // TODO: Fix this whole mess of having try/finally blocks in tests below which is an anti pattern. Instead
            // we need to separate tests by fixture.
            setup.rest().deletePage("XWiki", "InvitedMember");
            setup.rest().deletePage("XWiki", "AnotherInvitedMember");

            initialized = true;
        }

        setSenderPage(InvitationSenderPage.gotoPage());
        getSenderPage().fillInDefaultValues();
    }

    @Test
    @Order(1)
    void guestActionsOnNonexistantMessage(TestUtils setup)
    {
        TestUtils.Session s = setup.getSession();
        try {
            setup.forceGuestUser();

            // Try to accept nonexistent message.
            setup.gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_accept&messageID=12345");
            InvitationGuestActionsPage guestPage = new InvitationGuestActionsPage();
            assertNotNull(guestPage.getMessage(), "Guests able to accept nonexistent invitation");
            assertEquals("No message was found by the given ID. It might have been deleted "
                + "or maybe the system is experiencing difficulties.", guestPage.getMessage());

            // Try to decline nonexistent message.
            setup.gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_decline&messageID=12345");
            assertNotNull(guestPage.getMessage(), "Guests able to decline nonexistent invitation");
            assertEquals("No invitation was found by the given ID. It might have been deleted or "
                + "maybe the system is experiencing difficulties.", guestPage.getMessage());

            // Try to report nonexistent message.
            setup.gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_report&messageID=12345");
            assertNotNull(guestPage.getMessage(), "Guests able to report nonexistent invitation as spam");
            assertEquals("There was no message found by the given ID. Maybe an administrator "
                + "deleted the message from our system.", guestPage.getMessage());
        } finally {
            setup.setSession(s);
        }
    }

    @Test
    @Order(2)
    void sendMailToTwoAddresses() throws Exception
    {
        try {
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 2);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();

            assertEquals(2, messages.length, "wrong number of messages");

            // Correspond to messages a and b
            int a = 1;
            int b = 2;

            Map<String, String> messageA = getMessageContent(messages[0]);
            Map<String, String> messageB = getMessageContent(messages[1]);

            assertNotEquals(messageA.get("recipient"), messageB.get("recipient"),
                "Both messages are going to the same recipient");

            // No guarantee which message will come in first.
            if (messageA.get("recipient").contains("anotheruser@localhost.localdomain")) {
                Map<String, String> temp = messageB;
                messageB = messageA;
                messageA = temp;
                b = 1;
                a = 2;
            }

            assertTrue(messageA.get("recipient").contains("user@localhost.localdomain"),
                String.format("Wrong recipient name.\nExpecting:user@localhost.localdomain\n      Got:%s",
                    messageA.get("recipient")));

            assertTrue(messageB.get("recipient").contains("anotheruser@localhost.localdomain"),
                String.format("Wrong recipient name.\nExpecting:anotheruser@localhost.localdomain\n      Got:%s",
                    messageB.get("recipient")));

            assertMessageValid(messageA);
            assertMessageValid(messageB);

            // Check that the page has the table and the messages.
            assertTrue(sent.getMessageBoxContent().contains("Information\nYour message has been sent."));
            TableElement table = sent.getTable();
            assertEquals(3, table.numberOfRows());
            assertEquals(3, table.numberOfColumns());
            assertTrue(table.getRow(a).get(1).getText().contains("user@localhost.localdomain"));
            assertTrue(table.getRow(a).get(2).getText().contains("Pending"));
            assertTrue(table.getRow(b).get(1).getText().contains("anotheruser@localhost.localdomain"));
            assertTrue(table.getRow(b).get(2).getText().contains("Pending"));
        } finally {
            stopGreenMail();
        }
    }

    @Test
    @Order(3)
    void previewMessage()
    {
        InvitationMessageDisplayElement preview = getSenderPage().preview();
        assertTrue(preview.getSubjectLine().contains("has invited you to join"));
        assertTrue(preview.getMessageBody().contains("If this message looks like abuse of our system"));
        assertTrue(preview.getValidRecipients().get(0).getText().contains("user@localhost.localdomain"));
    }

    @Test
    @Order(4)
    void nonAdminCanSend(TestUtils setup) throws Exception
    {
        TestUtils.Session s = setup.getSession();
        try {
            setup.forceGuestUser();
            setup.createUserAndLogin("NonMailAdminUser", "WeakPassword");
            setSenderPage(InvitationSenderPage.gotoPage());
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();

            // Prove that the message was sent.
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            Map<String, String> message = getMessageContent(messages[0]);
            assertTrue(message.get("recipient").contains("user@localhost.localdomain"));
            assertMessageValid(message);

            // Check that the page has the table and the message.
            assertTrue(sent.getMessageBoxContent().contains("Your message has been sent."));
            TableElement table = sent.getTable();
            assertEquals(2, table.numberOfRows());
            assertEquals(3, table.numberOfColumns());
            assertTrue(table.getRow(1).get(1).getText().contains("user@localhost.localdomain"));
            assertTrue(table.getRow(1).get(2).getText().contains("Pending"));
        } finally {
            stopGreenMail();
            setup.setSession(s);
            setup.rest().deletePage("XWiki", "NonMailAdminUser");
        }
    }

    /**
     * This test proves that: 1. Non administrators trying to send to multiple email addresses without permission will
     * get an error message. and said mail will not be sent. 2. After permission is granted sending to multiple users
     * will work and message will say mail was sent.
     */
    @Test
    @Order(5)
    void unpermittedUserCannotSendToMultipleAddresses(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();

        // Make sure users don't have the right to send to multiple.
        AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
        config.getFormContainerElement().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
            + "usersMaySendToMultiple"), "false");
        config.clickSave(true);

        try {
            setup.forceGuestUser();
            setup.createUserAndLogin("NonMailAdminUser", "WeakPassword");
            setSenderPage(InvitationSenderPage.gotoPage());
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(2000, 2);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            assertEquals(0, messages.length, "Messages were received when they shouldn't have been sent!");
            assertEquals("Error\nYour message could not be sent because there were no valid email addresses to send to.",
                sent.getMessageBoxContent(),
                "User was not shown the correct error message.");
            stopGreenMail();

            // Become admin and allow users to send to multiple.
            TestUtils.Session nonAdmin = setup.getSession();
            setup.setSession(admin);
            config = AdministrationSectionPage.gotoPage("Invitation");
            config.getFormContainerElement().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "true");
            config.clickSave(true);
            setup.setSession(nonAdmin);

            // Prove that the user can now send to multiple recipients.
            startGreenMail();
            setSenderPage(InvitationSenderPage.gotoPage());
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 2);
            messages = getGreenMail().getReceivedMessages();
            assertEquals(2, messages.length, "Non admins cannot send mail to even with permission");
            assertEquals("Information\nYour message has been sent.", sent.getMessageBoxContent(),
                "User was not given the message that their mail was sent.");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
            setup.rest().deletePage("XWiki", "NonMailAdminUser");
        }
    }

    /**
     * This test proves that: 1. Guests (mail recipients) can report spam. 2. After a spam report, a user's mail
     * privilege is suspended. 3. An admin will see a message telling him that a spam report was made. 4. After an admin
     * marks the message as not spam, the sender can again send mail.
     */
    @Test
    @Order(6)
    void spamReporting(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            setup.forceGuestUser();
            setup.createUserAndLogin("spam", "andEggs");
            setSenderPage(InvitationSenderPage.gotoPage());
            startGreenMail();
            getSenderPage().fillForm("undisclosed-recipients@localhost.localdomain", null,
                "You have won the email lottery!");
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");

            // Restare greenmail to clear message
            stopGreenMail();
            startGreenMail();

            // Now switch to guest.
            TestUtils.Session spammer = setup.getSession();
            setup.forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            guestPage.setMemo("It's the email lottery, they have taken over your server!");
            guestPage.confirm();
            assertTrue(guestPage.getMessage().contains("Your report has been logged and the situation"),
                "Failed to report spam");

            // Prove that a reported message cannot be accepted (which would clear the "reported" status)
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertEquals("This invitation has been reported as spam and is no longer valid.", guestPage.getMessage(),
                "After a message is reported a user can accept it, clearing the spam report");
            // Prove that a reported message cannot be declined
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            assertEquals("This invitation has already been reported as "
                    + "spam and thus cannot be declined.", guestPage.getMessage(),
                "After a message is reported a user can decline it, clearing the spam report");
            // Switch to admin
            setup.setSession(admin);
            // Go to invitation sender.
            setSenderPage(InvitationSenderPage.gotoPage());
            // Switch back to spammer.
            setup.setSession(spammer);
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(2000, 1);
            assertEquals(0, getGreenMail().getReceivedMessages().length, "Reported spammers can send mail!");
            assertTrue(getSenderPage().userIsSpammer(), "No message telling user he's reported spammer.");

            // Switch to admin.
            setup.setSession(admin);
            setSenderPage(InvitationSenderPage.gotoPage());
            assertEquals(1, getSenderPage().getFooter().spamReports(),
                "No warning in footer that a message is reported as spam");
            // View spam message.
            InspectInvitationsPage inspectPage = getSenderPage().getFooter().inspectAllInvitations();
            InspectInvitationsPage.OneMessage inspect =
                inspectPage.getMessageWhere("Subject",
                    String.format("spam has invited you to join %s", new URL(setup.getBaseURL()).getHost()));
            // Prove that the memo left by spam reported is shown.
            String expectedMessage = "Reported as spam with message: It's the email lottery, they have taken over "
                + "your server!";
            assertEquals(expectedMessage, inspect.getStatusAndMemo(),
                "The message by the spam reporter is not shown to the admin.");

            String memo = "Actually the email lottery is quite legitimate.";
            String expectedSuccessMessage = "Information\nInvitation successfully marked as not spam. Log entry: " + memo;
            // Return their sending privilege.
            String successMessage = inspect.notSpam("Actually the email lottery is quite legitimate.");

            // Make sure the output is correct.
            assertEquals(expectedSuccessMessage, successMessage,
                "Admin got incorrect message after marking invitation as not spam\nExpecting:"
                    + expectedSuccessMessage + "\n      Got:" + successMessage);
            // Switch back to spammer
            setup.setSession(spammer);
            setSenderPage(InvitationSenderPage.gotoPage());
            assertFalse(getSenderPage().userIsSpammer(),
                "User permission to send not returned by admin action.");

            logCaptureConfiguration
                .registerExcludes("Login cookie validation hash mismatch! Cookies have been tampered with");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
            setup.rest().deletePage("XWiki", "spam");
        }
    }

    /**
     * This test proves that: 1. A guest can decline an invitation. 2. The message status changes and the footer
     * reflects this. 3. The sender can see the info box seeing the guest's reason for declining. 4. The message history
     * table shows the decline properly. 5. A guest cannot accept a message which has already been declined.
     */
    @Test
    @Order(7)
    void declineInvitation(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            assertEquals(1, getSenderPage().getFooter().myPendingInvitations(),
                "New invitation is not listed as pending in the footer.");
            // Now switch to guest.
            setup.forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            guestPage.setMemo("I'm not interested thank you.");
            guestPage.confirm();
            assertTrue(setup.getDriver().getPageSource().contains("This invitation has successfully been declined."),
                "Failed to decline invitation");
            // Switch to admin
            setup.setSession(admin);
            // Go to invitation sender.
            setSenderPage(InvitationSenderPage.gotoPage());
            assertEquals(0, getSenderPage().getFooter().spamReports(),
                "Declined invitation is still listed as pending in the footer.");

            // View declined invitation.
            InspectInvitationsPage inspectPage = getSenderPage().getFooter().inspectMyInvitations();
            InspectInvitationsPage.OneMessage inspect = inspectPage.getMessageWhere("Status", "Declined");

            assertEquals("Declined with message: I'm not interested thank you.", inspect.getStatusAndMemo(),
                "Not showing message box to say the invitation has been declined");

            // Ensure the message history table is correct.
            TableElement messageHistoryTable = inspect.clickMessageHistory();
            List<WebElement> row2 = messageHistoryTable.getRow(2);
            assertEquals("Declined", row2.get(0).getText(), "Message history table not showing correctly.");
            assertEquals("I'm not interested thank you.", row2.get(2).getText(),
                "Message history table not showing correctly.");

            // Make sure a guest can't accept the invitation now.
            setup.forceGuestUser();
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertEquals("This invitation has been declined and cannot be accepted now.", guestPage.getMessage(),
                "After a message is declined a user can still accept it!");
            // Try to decline the invitation.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            assertEquals("This invitation has already been declined and cannot be declined again.",
                guestPage.getMessage(), "User was allowed to decline an invitation twice.");
            // Prove that the message can still be reported as spam.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            assertEquals("", guestPage.getMessage(),
                "After the invitation was declined it now cannot be reported as spam.");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
        }
    }

    /**
     * This test proves that: 1. The accept invitation link sent in the email will work. 2. A user can accept an
     * invitation and be directed to the registration form and can register and login. 3. An invitation once accepted
     * cannot be accepted again nor declined. 4. An invitation once accepted can still be reported as spam.
     */
    @Test
    @Order(8)
    void acceptInvitation(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            assertEquals(1, getSenderPage().getFooter().myPendingInvitations(),
                "New invitation is not listed as pending in the footer.");
            // Now switch to guest.
            setup.forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertEquals("", guestPage.getMessage(),
                String.format("There was an error message when accepting the invitation message:\n%s",
                    guestPage.getMessage()));
            // Register a new user.
            RegistrationPage rp = new RegistrationPage();
            rp.fillRegisterForm(null, null, "InvitedMember", "WeakPassword", "WeakPassword", null);
            rp.clickRegister();
            assertTrue(rp.getValidationFailureMessages().isEmpty(), "There were failure messages when registering.");
            setup.getDriver().get(setup.getURLToLoginAs("InvitedMember", "WeakPassword"));

            assertTrue(rp.isAuthenticated(), "Failed to log user in after registering from invitation.");

            // Now switch to guest again and try to accept the invitation again.
            setup.forceGuestUser();
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertEquals("This invitation has already been accepted and the "
                    + "offer is no longer valid.", guestPage.getMessage(),
                "After the invitation was accepted a user was allowed to accept it again.");
            // Try to decline the invitation.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            assertEquals("This invitation has already been accepted and "
                    + "now cannot be declined.", guestPage.getMessage(),
                "After the invitation was accepted a user was allowed to decline it.");
            // Prove that the message can still be reported as spam
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            assertEquals("", guestPage.getMessage(),
                "After the invitation was accepted it now cannot be reported as spam.");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
        }
    }

    /**
     * This test proves that: 1. A guest cannot register if register permission is removed from XWikiPreferences. 2.
     * Upon receiving an email invitation the guest can register even without register permission.
     */
    @Test
    @Order(9)
    void acceptInvitationToClosedWiki(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            // First we ban anon from registering.
            setup.setGlobalRights("", "XWiki.XWikiGuest", "register", false);
            // now prove anon cannot register
            setup.forceGuestUser();
            RegistrationPage.gotoPage();
            setup.assertOnPage(setup.getURL("XWiki", "XWikiLogin", "login"));

            // Now we try sending and accepting an invitation.
            setup.setSession(admin);
            setSenderPage(InvitationSenderPage.gotoPage());
            getSenderPage().fillInDefaultValues();

            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            assertEquals(1, getSenderPage().getFooter().myPendingInvitations(),
                "New invitation is not listed as pending in the footer.");
            // Now switch to guest.
            setup.forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertEquals("", guestPage.getMessage(),
                "There was an error message when accepting the invitation message:\n"
                    + guestPage.getMessage());
            // Register a new user.
            RegistrationPage rp = new RegistrationPage();
            rp.fillRegisterForm(null, null, "AnotherInvitedMember", "WeakPassword", "WeakPassword", null);
            rp.clickRegister();
            assertTrue(rp.getValidationFailureMessages().isEmpty(),
                "There were failure messages when registering.");
            setup.getDriver().get(setup.getURLToLoginAs("AnotherInvitedMember", "WeakPassword"));

            assertTrue(rp.isAuthenticated(), "Failed to log user in after registering from invitation.");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
            setup.deleteObject("XWiki", "XWikiPreferences", "XWiki.XWikiGlobalRights", 0);
        }
    }

    /**
     * This test proves that: 1. A user can cancel an invitation after sending it, leaving a message for the recipient
     * should they try to accept. 2. A canceled invitation cannot be accepted and the guest will see an explaination
     * with the message left when the sender canceled. 3. A canceled invitation cannot be declined, the guest gets the
     * sender's note. 4. A canceled invitation can still be reported as spam.
     */
    @Test
    @Order(10)
    void cancelInvitation(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            assertEquals(1, getSenderPage().getFooter().myPendingInvitations(),
                "New invitation is not listed as pending in the footer.");

            InspectInvitationsPage.OneMessage message = getSenderPage().getFooter().inspectMyInvitations()
                .getMessageWhere("Subject",
                    String.format("superadmin has invited you to join %s This is a subject line.",
                        new URL(setup.getBaseURL()).getHost()));

            InvitationActionConfirmationElement confirm = message.cancel();

            assertEquals("leave a message in case the invitee(s) try to register.",
                confirm.getLabel().toLowerCase());

            confirm.setMemo("Sorry, wrong email address.");
            assertEquals("Information\nInvitation successfully rescinded.", confirm.confirm());

            // Now switch to guest.
            setup.forceGuestUser();

            String commonPart = "\nsuperadmin left you this message when rescinding the invitation.\n"
                + "Sorry, wrong email address.";

            // Prove that invitation cannot be accepted
            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            assertNotEquals("", guestPage.getMessage(), "Guest was able to accept a message which had been canceled.");
            assertEquals("Error\nWe're sorry but this invitation has been rescinded." + commonPart,
                guestPage.getMessage());

            // Prove that invitation cannot be declined
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            assertNotEquals("", guestPage.getMessage(), "Guest was able to decline a message which had been canceled.");
            assertEquals("Error\nThis invitation has been rescinded and thus cannot be declined." + commonPart,
                guestPage.getMessage());

            // Prove that the message report spam page still shows up.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            assertEquals("", guestPage.getMessage(), "Guest was not able to report canceled invitation as spam");
            guestPage.setMemo("Canceled message is spam.");
            assertEquals("Your report has been logged and the situation will "
                + "be investigated as soon as possible, we apologize for the inconvenience.", guestPage.confirm());
        } finally {
            stopGreenMail();
            setup.setSession(admin);
        }
    }

    /**
     * This test proves that: 1. A user cannot send to the same address multiple (8000) times which would be very
     * annoying for the recipient.
     */
    @Test
    @Order(11)
    void sendManyToOneAddress(TestUtils setup) throws Exception
    {
        TestUtils.Session admin = setup.getSession();
        try {
            // Allow users to send to multiple.
            AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
            config.getFormContainerElement().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "true");
            config.clickSave(true);

            // Now switch to a wizeguy user
            setup.forceGuestUser();
            setup.createUserAndLogin("tr0ll", "StrongPassword");
            setSenderPage(InvitationSenderPage.gotoPage());

            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain user@localhost.localdomain "
                + "user@localhost.localdomain user@localhost.localdomain", null, null);
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            assertEquals(1, messages.length, "One user is able to send multiple messages to the same poor recipient.");
        } finally {
            stopGreenMail();
            setup.setSession(admin);
            setup.rest().deletePage("XWiki", "tr0ll");
        }
    }

    //-----------------------Helper methods--------------------------//

    /**
     * To put the page someplace else, subclass this class and change this method.
     */
    private InvitationSenderPage newSenderPage()
    {
        return new InvitationSenderPage();
    }

    private void assertMessageValid(Map<String, String> message)
    {
        assertTrue(message.get("htmlPart").contains("If this message looks like abuse of our system"));
        assertTrue(message.get("subjectLine").contains("has invited you to join"));
    }

    private Map<String, String> getMessageContent(MimeMessage message) throws Exception
    {
        Map<String, String> messageMap = new HashMap<>();

        Address[] addresses = message.getAllRecipients();
        assertEquals(1, addresses.length);
        messageMap.put("recipient", addresses[0].toString());

        messageMap.put("subjectLine", message.getSubject());

        Multipart mp = (Multipart) message.getContent();

        BodyPart plain = getPart(mp, "text/plain");
        if (plain != null) {
            messageMap.put("textPart", plain.getContent().toString());
        }
        BodyPart html = getPart(mp, "text/html");
        if (html != null) {
            messageMap.put("htmlPart", html.getContent().toString());
        }

        return messageMap;
    }

    private BodyPart getPart(Multipart messageContent, String mimeType) throws Exception
    {
        for (int i = 0; i < messageContent.getCount(); i++) {
            BodyPart part = messageContent.getBodyPart(i);

            if (part.isMimeType(mimeType)) {
                return part;
            }

            if (part.isMimeType("multipart/related")
                || part.isMimeType("multipart/alternative")
                || part.isMimeType("multipart/mixed"))
            {
                BodyPart out = getPart((Multipart) part.getContent(), mimeType);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    private void startGreenMail()
    {
        this.greenMail = new GreenMail(ServerSetupTest.SMTP);
        this.greenMail.start();
    }

    private void stopGreenMail()
    {
        if (getGreenMail() != null) {
            getGreenMail().stop();
        }
    }

    private GreenMail getGreenMail()
    {
        return this.greenMail;
    }

    private InvitationSenderPage getSenderPage()
    {
        return this.senderPage;
    }

    private void setSenderPage(InvitationSenderPage senderPage)
    {
        this.senderPage = senderPage;
    }

    private void configureEmail(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host",
            testConfiguration.getServletEngine().getHostIP(), "port", "3025", "sendWaitTime", "0");
    }
}
