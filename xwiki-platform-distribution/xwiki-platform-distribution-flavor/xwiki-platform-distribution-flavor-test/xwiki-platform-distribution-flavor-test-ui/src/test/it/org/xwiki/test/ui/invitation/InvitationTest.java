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
package org.xwiki.test.ui.invitation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.invitation.test.po.InspectInvitationsPage;
import org.xwiki.invitation.test.po.InvitationActionConfirmationElement;
import org.xwiki.invitation.test.po.InvitationGuestActionsPage;
import org.xwiki.invitation.test.po.InvitationMessageDisplayElement;
import org.xwiki.invitation.test.po.InvitationSenderPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.RegistrationPage;
import org.xwiki.test.ui.po.TableElement;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import com.icegreen.greenmail.util.GreenMail;

/**
 * Tests invitation application.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class InvitationTest extends AbstractTest
{
    private static boolean initialized;

    private InvitationSenderPage senderPage;

    private GreenMail greenMail;

    @Before
    public void setUp() throws Exception
    {
        // Login as admin and delete existing messages.
        getDriver().get(getUtil().getURLToLoginAsAdminAndGotoPage(getUtil().getURLToNonExistentPage()));
        getUtil().recacheSecretToken();
        getUtil().setDefaultCredentials(TestUtils.ADMIN_CREDENTIALS);
        getUtil().rest().deletePage("Invitation", "InvitationMessages");

        if (!initialized) {
            // We have to go to sender page before any config shows up.
            InvitationSenderPage.gotoPage();

            AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
            // Set port to 3025
            config.getForm().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_smtp_port"),
                "3025");
            // Make sure that by default we don't allow non admin to send emails to multiple addresses
            config.getForm().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "false");
            config.clickSave();

            // Make sure the users we're registering in testAcceptInvitation and testAcceptInvitationToCloseWiki don't
            // exist.
            // TODO: Fix this whole mess of having try/finally blocks in tests below which is an anti pattern. Instead
            // we need to separate tests by fixture.
            getUtil().rest().deletePage("XWiki", "InvitedMember");
            getUtil().rest().deletePage("XWiki", "AnotherInvitedMember");

            initialized = true;
        }

        setSenderPage(InvitationSenderPage.gotoPage());
        getSenderPage().fillInDefaultValues();
    }

    @Test
    public void testGuestActionsOnNonexistantMessage() throws Exception
    {
        TestUtils.Session s = getUtil().getSession();
        try {
            getUtil().forceGuestUser();

            // Try to accept nonexistent message.
            getUtil().gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_accept&messageID=12345");
            InvitationGuestActionsPage guestPage = new InvitationGuestActionsPage();
            Assert.assertNotNull("Guests able to accept nonexistent invitation", guestPage.getMessage());
            Assert.assertEquals("No message was found by the given ID. It might have been deleted "
                + "or maybe the system is experiencing difficulties.", guestPage.getMessage());

            // Try to decline nonexistent message.
            getUtil().gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_decline&messageID=12345");
            Assert.assertNotNull("Guests able to decline nonexistent invitation", guestPage.getMessage());
            Assert.assertEquals("No invitation was found by the given ID. It might have been deleted or "
                + "maybe the system is experiencing difficulties.", guestPage.getMessage());

            // Try to report nonexistent message.
            getUtil().gotoPage("Invitation", "InvitationGuestActions", "view", "doAction_report&messageID=12345");
            Assert.assertNotNull("Guests able to report nonexistent invitation as spam", guestPage.getMessage());
            Assert.assertEquals("There was no message found by the given ID. Maybe an administrator "
                + "deleted the message from our system.", guestPage.getMessage());
        } finally {
            getUtil().setSession(s);
        }
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testSendMailToTwoAddresses() throws Exception
    {
        try {
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 2);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();

            Assert.assertTrue("wrong number of messages", messages.length == 2);

            // Correspond to messages a and b
            int a = 1, b = 2;

            Map<String, String> messageA = getMessageContent(messages[0]);
            Map<String, String> messageB = getMessageContent(messages[1]);

            Assert.assertFalse("Both messages are going to the same recipient",
                messageA.get("recipient").equals(messageB.get("recipient")));

            // No guarentee which message will come in first.
            if (messageA.get("recipient").contains("anotheruser@localhost.localdomain")) {
                Map<String, String> temp = messageB;
                messageB = messageA;
                messageA = temp;
                b = 1;
                a = 2;
            }

            Assert.assertTrue("Wrong recipient name.\nExpecting:user@localhost.localdomain\n      Got:"
                + messageA.get("recipient"),
                messageA.get("recipient").contains("user@localhost.localdomain"));

            Assert.assertTrue("Wrong recipient name.\nExpecting:anotheruser@localhost.localdomain\n      Got:"
                + messageB.get("recipient"),
                messageB.get("recipient").contains("anotheruser@localhost.localdomain"));

            assertMessageValid(messageA);
            assertMessageValid(messageB);

            // Check that the page has the table and the messages.
            Assert.assertTrue(sent.getMessageBoxContent().contains("Your message has been sent."));
            TableElement table = sent.getTable();
            Assert.assertTrue(table.numberOfRows() == 3);
            Assert.assertTrue(table.numberOfColumns() == 3);
            Assert.assertTrue(table.getRow(a).get(1).getText().contains("user@localhost.localdomain"));
            Assert.assertTrue(table.getRow(a).get(2).getText().contains("Pending"));
            Assert.assertTrue(table.getRow(b).get(1).getText().contains("anotheruser@localhost.localdomain"));
            Assert.assertTrue(table.getRow(b).get(2).getText().contains("Pending"));
        } finally {
            stopGreenMail();
        }
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testPreviewMessage()
    {
        InvitationMessageDisplayElement preview = getSenderPage().preview();
        Assert.assertTrue(preview.getSubjectLine().contains("has invited you to join"));
        Assert.assertTrue(preview.getMessageBody().contains("If this message looks like abuse of our system"));
        Assert.assertTrue(preview.getValidRecipients().get(0).getText().contains("user@localhost.localdomain"));
    }

    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testNonAdminCanSend() throws Exception
    {
        TestUtils.Session s = getUtil().getSession();
        try {
            getUtil().forceGuestUser();
            getUtil().createUserAndLogin("NonMailAdminUser", "WeakPassword");
            setSenderPage(InvitationSenderPage.gotoPage());
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();

            // Prove that the message was sent.
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            Map<String, String> message = getMessageContent(messages[0]);
            Assert.assertTrue(message.get("recipient").contains("user@localhost.localdomain"));
            assertMessageValid(message);

            // Check that the page has the table and the message.
            Assert.assertTrue(sent.getMessageBoxContent().contains("Your message has been sent."));
            TableElement table = sent.getTable();
            Assert.assertTrue(table.numberOfRows() == 2);
            Assert.assertTrue(table.numberOfColumns() == 3);
            Assert.assertTrue(table.getRow(1).get(1).getText().contains("user@localhost.localdomain"));
            Assert.assertTrue(table.getRow(1).get(2).getText().contains("Pending"));
        } finally {
            stopGreenMail();
            getUtil().setSession(s);
            getUtil().rest().deletePage("XWiki", "NonMailAdminUser");
        }
    }

    /**
     * This test proves that: 1. Non administrators trying to send to multiple email addresses without permission will
     * get an error message. and said mail will not be sent. 2. After permission is granted sending to multiple users
     * will work and message will say mail was sent.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testUnpermittedUserCannotSendToMultipleAddresses() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();

        // Make sure users don't have the right to send to multiple.
        AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
        config.getForm().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
            + "usersMaySendToMultiple"), "false");
        config.clickSave();

        try {
            getUtil().forceGuestUser();
            getUtil().createUserAndLogin("NonMailAdminUser", "WeakPassword");
            setSenderPage(InvitationSenderPage.gotoPage());
            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            InvitationSenderPage.InvitationSentPage sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(2000, 2);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            Assert.assertTrue("Messages were received when they shouldn't have been sent!", messages.length == 0);
            Assert.assertEquals("User was not shown the correct error message.",
                "Your message could not be sent because there were no valid email addresses to send to.",
                sent.getMessageBoxContent());
            stopGreenMail();

            // Become admin and allow users to send to multiple.
            TestUtils.Session nonAdmin = getUtil().getSession();
            getUtil().setSession(admin);
            config = AdministrationSectionPage.gotoPage("Invitation");
            config.getForm().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "true");
            config.clickSave();
            getUtil().setSession(nonAdmin);

            // Prove that the user can now send to multiple recipients.
            startGreenMail();
            setSenderPage(InvitationSenderPage.gotoPage());
            getSenderPage().fillForm("user@localhost.localdomain anotheruser@localhost.localdomain", null, null);
            sent = getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 2);
            messages = getGreenMail().getReceivedMessages();
            Assert.assertTrue("Non admins cannot send mail to even with permission", messages.length == 2);
            Assert.assertTrue("User was not given the message that their mail was sent.",
                sent.getMessageBoxContent().equals("Your message has been sent."));
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
            getUtil().rest().deletePage("XWiki", "NonMailAdminUser");
        }
    }

    /** 
     * This test proves that:
     * 1. Guests (mail recipients) can report spam.
     * 2. After a spam report, a user's mail privilege is suspended.
     * 3. An admin will see a message telling him that a spam report was made.
     * 4. After an admin marks the message as not spam, the sender can again send mail.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testSpamReporting() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            getUtil().forceGuestUser();
            getUtil().createUserAndLogin("spam", "andEggs");
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
            TestUtils.Session spammer = getUtil().getSession();
            getUtil().forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            guestPage.setMemo("It's the email lottery, they have taken over your server!");
            guestPage.confirm();
            Assert.assertTrue("Failed to report spam",
                guestPage.getMessage().contains("Your report has been logged and the situation"));

            // Prove that a reported message cannot be accepted (which would clear the "reported" status)
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertTrue("After a message is reported a user can accept it, clearing the spam report",
                guestPage.getMessage().equals("This invitation has been reported as spam and is no longer valid."));
            // Prove that a reported message cannot be declined
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            Assert.assertTrue("After a message is reported a user can decline it, clearing the spam report",
                guestPage.getMessage().equals("This invitation has already been reported as "
                + "spam and thus cannot be declined."));
            // Switch to admin
            getUtil().setSession(admin);
            // Go to invitation sender.
            setSenderPage(InvitationSenderPage.gotoPage());
            // Switch back to spammer.
            getUtil().setSession(spammer);
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(2000, 1);
            Assert.assertTrue("Reported spammers can send mail!", getGreenMail().getReceivedMessages().length == 0);
            Assert.assertTrue("No message telling user he's reported spammer.", getSenderPage().userIsSpammer());

            // Switch to admin.
            getUtil().setSession(admin);
            setSenderPage(InvitationSenderPage.gotoPage());
            Assert.assertTrue("No warning in footer that a message is reported as spam",
                getSenderPage().getFooter().spamReports() == 1);
            // View spam message.
            InspectInvitationsPage inspectPage = getSenderPage().getFooter().inspectAllInvitations();
            InspectInvitationsPage.OneMessage inspect =
                inspectPage.getMessageWhere("Subject", "spam has invited you to join localhost");
            // Prove that the memo left by spam reported is shown.
            String expectedMessage = "Reported as spam with message: It's the email lottery, they have taken over "
                + "your server!";
            Assert.assertTrue("The message by the spam reporter is not shown to the admin.\nExpecting:"
                + expectedMessage + "\n      Got:" + inspect.getStatusAndMemo(),
                inspect.getStatusAndMemo().equals(expectedMessage));

            String memo = "Actually the email lottery is quite legitimate.";
            String expectedSuccessMessage = "Invitation successfully marked as not spam. Log entry: " + memo;
            // Return their sending privilege.
            String successMessage = inspect.notSpam("Actually the email lottery is quite legitimate.");

            // Make sure the output is correct.
            Assert.assertTrue("Admin got incorrect message after marking invitation as not spam\nExpecting:"
                + expectedSuccessMessage + "\n      Got:" + successMessage,
                expectedSuccessMessage.equals(successMessage));
            // Switch back to spammer
            getUtil().setSession(spammer);
            setSenderPage(InvitationSenderPage.gotoPage());
            Assert.assertFalse("User permission to send not returned by admin action.",
                getSenderPage().userIsSpammer());
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
            getUtil().rest().deletePage("XWiki", "spam");
        }
    }

    /** 
     * This test proves that:
     * 1. A guest can decline an invitation.
     * 2. The message status changes and the footer reflects this.
     * 3. The sender can see the info box seeing the guest's reason for declining.
     * 4. The message history table shows the decline properly.
     * 5. A guest cannot accept a message which has already been declined.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDeclineInvitation() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            Assert.assertTrue("New invitation is not listed as pending in the footer.",
                getSenderPage().getFooter().myPendingInvitations() == 1);
            // Now switch to guest.
            getUtil().forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            guestPage.setMemo("I'm not interested thank you.");
            guestPage.confirm();
            Assert.assertTrue("Failed to decline invitation",
                getDriver().getPageSource().contains("This invitation has successfully been declined."));
            // Switch to admin
            getUtil().setSession(admin);
            // Go to invitation sender.
            setSenderPage(InvitationSenderPage.gotoPage());
            Assert.assertTrue("Declined invitation is still listed as pending in the footer.",
                getSenderPage().getFooter().spamReports() == 0);

            // View declined invitation.
            InspectInvitationsPage inspectPage = getSenderPage().getFooter().inspectMyInvitations();
            InspectInvitationsPage.OneMessage inspect = inspectPage.getMessageWhere("Status", "Declined");

            Assert.assertTrue("Not showing message box to say the invitation has been declined",
                inspect.getStatusAndMemo().equals("Declined with message: I'm not interested thank you."));

            // Insure the message history table is correct.
            TableElement messageHistoryTable = inspect.clickMessageHistory();
            List<WebElement> row2 = messageHistoryTable.getRow(2);
            Assert.assertTrue("Message history table not showing correctly.",
                row2.get(0).getText().equals("Declined"));
            Assert.assertTrue("Message history table not showing correctly.",
                row2.get(2).getText().equals("I'm not interested thank you."));

            // Make sure a guest can't accept the invitation now.
            getUtil().forceGuestUser();
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertTrue("After a message is declined a user can still accept it!",
                guestPage.getMessage().equals("This invitation has been declined and cannot be accepted now."));
            // Try to decline the invitation.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            Assert.assertTrue("User was allowed to decline an invitation twice.",
                guestPage.getMessage().equals("This invitation has already been declined and "
                + "cannot be declined again."));
            // Prove that the message can still be reported as spam
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            Assert.assertTrue("After the invitation was declined it now cannot be reported as spam.",
                guestPage.getMessage().equals(""));
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
        }
    }

    /**
     * This test proves that:
     * 1. The accept invitation link sent in the email will work.
     * 2. A user can accept an invitation and be directed to the registration form and can register and login.
     * 3. An invitation once accepted cannot be accepted again nor declined.
     * 4. An invitation once accepted can still be reported as spam.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testAcceptInvitation() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            Assert.assertTrue("New invitation is not listed as pending in the footer.",
                getSenderPage().getFooter().myPendingInvitations() == 1);
            // Now switch to guest.
            getUtil().forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertTrue("There was an error message when accepting the invitation message:\n"
                + guestPage.getMessage(),
                guestPage.getMessage().equals(""));
            // Register a new user.
            RegistrationPage rp = new RegistrationPage();
            rp.fillRegisterForm(null, null, "InvitedMember", "WeakPassword", "WeakPassword", null);
            rp.clickRegister();
            Assert.assertTrue("There were failure messages when registering.",
                rp.getValidationFailureMessages().isEmpty());
            getDriver().get(getUtil().getURLToLoginAs("InvitedMember", "WeakPassword"));

            Assert.assertTrue("Failed to log user in after registering from invitation.", rp.isAuthenticated());

            // Now switch to guest again and try to accept the invitation again.
            getUtil().forceGuestUser();
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertTrue("After the invitation was accepted a user was allowed to accept it again.",
                guestPage.getMessage().equals("This invitation has already been accepted and the "
                + "offer is no longer valid."));
            // Try to decline the invitation.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            Assert.assertTrue("After the invitation was accepted a user was allowed to decline it.",
                guestPage.getMessage().equals("This invitation has already been accepted and "
                + "now cannot be declined."));
            // Prove that the message can still be reported as spam
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            Assert.assertTrue("After the invitation was accepted it now cannot be reported as spam.",
                guestPage.getMessage().equals(""));
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
        }
    }

    /**
     * This test proves that:
     * 1. A guest cannot register if register permission is removed from XWikiPreferences.
     * 2. Upon receiving an email invitation the guest can register even without register permission.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testAcceptInvitationToClosedWiki() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            // First we ban anon from registering.
            ObjectEditPage oep = ObjectEditPage.gotoPage("XWiki", "XWikiPreferences");

            oep.getObjectsOfClass("XWiki.XWikiGlobalRights").get(0)
                .getSelectElement(By.name("XWiki.XWikiGlobalRights_0_levels")).select("register");

            oep.clickSaveAndContinue();
            // now prove anon cannot register
            getUtil().forceGuestUser();
            RegistrationPage.gotoPage();
            getUtil().assertOnPage(getUtil().getURL("XWiki", "XWikiLogin", "login"));

            // Now we try sending and accepting an invitation.
            getUtil().setSession(admin);
            setSenderPage(InvitationSenderPage.gotoPage());
            getSenderPage().fillInDefaultValues();

            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            Assert.assertTrue("New invitation is not listed as pending in the footer.",
                getSenderPage().getFooter().myPendingInvitations() == 1);
            // Now switch to guest.
            getUtil().forceGuestUser();

            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertTrue("There was an error message when accepting the invitation message:\n"
                + guestPage.getMessage(),
                guestPage.getMessage().equals(""));
            // Register a new user.
            RegistrationPage rp = new RegistrationPage();
            rp.fillRegisterForm(null, null, "AnotherInvitedMember", "WeakPassword", "WeakPassword", null);
            rp.clickRegister();
            Assert.assertTrue("There were failure messages when registering.",
                rp.getValidationFailureMessages().isEmpty());
            getDriver().get(getUtil().getURLToLoginAs("AnotherInvitedMember", "WeakPassword"));

            Assert.assertTrue("Failed to log user in after registering from invitation.", rp.isAuthenticated());
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);

            // Better open the wiki back up again.
            ObjectEditPage oep = ObjectEditPage.gotoPage("XWiki", "XWikiPreferences");

            oep.getObjectsOfClass("XWiki.XWikiGlobalRights").get(0)
                .getSelectElement(By.name("XWiki.XWikiGlobalRights_0_levels")).unSelect("register");

            oep.clickSaveAndContinue();
        }
    }

    /**
     * This test proves that:
     * 1. A user can cancel an invitation after sending it, leaving a message for the recipient should they try to 
     *    accept.
     * 2. A canceled invitation cannot be accepted and the guest will see an explaination with the message left when
     *    the sender canceled.
     * 3. A canceled invitation cannot be declined, the guest gets the sender's note.
     * 4. A canceled invitation can still be reported as spam.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testCancelInvitation() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            startGreenMail();
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            String htmlMessage = getMessageContent(messages[0]).get("htmlPart");
            Assert.assertTrue("New invitation is not listed as pending in the footer.",
                getSenderPage().getFooter().myPendingInvitations() == 1);

            InspectInvitationsPage.OneMessage message = getSenderPage().getFooter().inspectMyInvitations()
                .getMessageWhere("Subject", "Admin has invited you to join localhost This is a subject line.");

            InvitationActionConfirmationElement confirm = message.cancel();

            Assert.assertEquals("leave a message in case the invitee(s) try to register.",
                confirm.getLabel().toLowerCase());

            confirm.setMemo("Sorry, wrong email address.");
            Assert.assertEquals("Invitation successfully rescinded.", confirm.confirm());

            // Now switch to guest.
            getUtil().forceGuestUser();

            String commonPart = "\nAdministrator left you this message when rescinding the invitation.\n"
                + "Sorry, wrong email address.";

            // Prove that invitation cannot be accepted
            InvitationGuestActionsPage guestPage =
                InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.ACCEPT);
            Assert.assertFalse("Guest was able to accept a message which had been canceled.",
                guestPage.getMessage().equals(""));
            Assert.assertEquals("We're sorry but this invitation has been rescinded." + commonPart,
                guestPage.getMessage());

            // Prove that invitation cannot be declined
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.DECLINE);
            Assert.assertFalse("Guest was able to decline a message which had been canceled.",
                guestPage.getMessage().equals(""));
            Assert.assertEquals("This invitation has been rescinded and thus cannot be declined." + commonPart,
                guestPage.getMessage());

            // Prove that the message report spam page still shows up.
            guestPage = InvitationGuestActionsPage.gotoPage(htmlMessage, InvitationGuestActionsPage.Action.REPORT);
            Assert.assertTrue("Guest was not able to report canceled invitation as spam",
                guestPage.getMessage().equals(""));
            guestPage.setMemo("Canceled message is spam.");
            Assert.assertEquals("Your report has been logged and the situation will "
                + "be investigated as soon as possible, we apologize for the inconvenience.", guestPage.confirm());
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
        }
    }

    /**
     * This test proves that:
     * 1. A user cannot send to the same address multiple (8000) times which would be very annoying for the recipient.
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    public void testSendManyToOneAddress() throws Exception
    {
        TestUtils.Session admin = getUtil().getSession();
        try {
            // Allow users to send to multiple.
            AdministrationSectionPage config = AdministrationSectionPage.gotoPage("Invitation");
            config.getForm().setFieldValue(By.id("Invitation.InvitationConfig_Invitation.WebHome_0_"
                + "usersMaySendToMultiple"), "true");
            config.clickSave();

            // Now switch to a wizeguy user
            getUtil().forceGuestUser();
            getUtil().createUserAndLogin("tr0ll", "StrongPassword");
            setSenderPage(InvitationSenderPage.gotoPage());

            startGreenMail();
            getSenderPage().fillForm("user@localhost.localdomain user@localhost.localdomain "
                + "user@localhost.localdomain user@localhost.localdomain", null, null);
            getSenderPage().send();
            getGreenMail().waitForIncomingEmail(10000, 1);
            MimeMessage[] messages = getGreenMail().getReceivedMessages();
            Assert.assertTrue("One user is able to send multiple messages to the same poor recipient.",
                messages.length == 1);
        } finally {
            stopGreenMail();
            getUtil().setSession(admin);
            getUtil().rest().deletePage("XWiki", "tr0ll");
        }
    }

    //-----------------------Helper methods--------------------------//


    /** To put the page someplace else, subclass this class and change this method. */
    protected InvitationSenderPage newSenderPage()
    {
        return new InvitationSenderPage();
    }

    protected void assertMessageValid(Map<String, String> message)
    {
        Assert.assertTrue(message.get("htmlPart").contains("If this message looks like abuse of our system"));
        Assert.assertTrue(message.get("subjectLine").contains("has invited you to join"));
    }

    protected Map<String, String> getMessageContent(MimeMessage message) throws Exception
    {
        Map<String, String> messageMap = new HashMap<String, String>();

        Address[] addresses = message.getAllRecipients();
        Assert.assertTrue(addresses.length == 1);
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

    protected BodyPart getPart(Multipart messageContent, String mimeType) throws Exception
    {
        for (int i = 0; i < messageContent.getCount(); i++) {
            BodyPart part = messageContent.getBodyPart(i);

            if (part.isMimeType(mimeType)) {
                return part;
            }

            if (part.isMimeType("multipart/related")
                || part.isMimeType("multipart/alternative")
                || part.isMimeType("multipart/mixed")) {
                BodyPart out = getPart((Multipart) part.getContent(), mimeType);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    protected void startGreenMail() throws Exception
    {
        this.greenMail = new GreenMail();
        this.greenMail.start();
    }

    protected void stopGreenMail() throws Exception
    {
        if (getGreenMail() != null) {
            getGreenMail().stop();
        }
    }

    protected GreenMail getGreenMail()
    {
        return this.greenMail;
    }

    protected InvitationSenderPage getSenderPage()
    {
        return this.senderPage;
    }

    protected void setSenderPage(InvitationSenderPage senderPage)
    {
        this.senderPage = senderPage;
    }
}
