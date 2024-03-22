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
package org.xwiki.security.authentication.test.ui;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.ForgotUsernameCompletePage;
import org.xwiki.administration.test.po.ForgotUsernamePage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the ForgotUsername UI.
 *
 * @version $Id$
 * @since 11.10
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        // The Mail module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-mail-send-storage"
    }
)
class ForgotUsernameIT
{
    private GreenMail mail;

    @BeforeEach
    public void startMail(TestUtils setup, TestConfiguration testConfiguration)
    {
        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();

        configureEmail(setup, testConfiguration);
    }

    @AfterEach
    public void stopMail(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration)
    {
        if (this.mail != null) {
            this.mail.stop();
        }

        restoreSettings(setup);
        logCaptureConfiguration.registerExcludes("CSRFToken: Secret token verification failed, token");
    }

    private void configureEmail(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host",
            testConfiguration.getServletEngine().getHostIP(), "port", "3025", "sendWaitTime", "0");
    }

    private void restoreSettings(TestUtils setup)
    {
        // Make sure we can restore the settings, so we log back with superadmin to finish the work
        setup.loginAsSuperAdmin();

        // Remove the previous version that the setup has created.
        setup.deleteLatestVersion("Mail", "MailConfig");
    }

    private Map<String, String> getMessageContent(MimeMessage message) throws Exception
    {
        Map<String, String> messageMap = new HashMap<>();

        Address[] addresses = message.getAllRecipients();
        assertTrue(addresses.length == 1);
        messageMap.put("recipient", addresses[0].toString());

        messageMap.put("subjectLine", message.getSubject());

        Multipart mp = (Multipart) message.getContent();

        BodyPart plain = getPart(mp, "text/plain");
        if (plain != null) {
            messageMap.put("textPart", IOUtils.toString(plain.getInputStream(), "UTF-8"));
        }
        BodyPart html = getPart(mp, "text/html");
        if (html != null) {
            messageMap.put("htmlPart", IOUtils.toString(html.getInputStream(), "UTF-8"));
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

            if (part.isMimeType("multipart/related") || part.isMimeType("multipart/alternative")
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

    @Test
    void retrieveUsername(TestUtils testUtils) throws Exception
    {
        // We create three users, two of them are sharing the same email
        String user1Login = "realuser1";
        String user1Email = "realuser@host.org";

        String user2Login = "realuser2";
        String user2Email = "realuser@host.org";

        String user3Login = "foo";
        String user3Email = "foo@host.org";

        // We need to login as superadmin to set the user email.
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(user1Login, "realuserpwd", testUtils.getURLToNonExistentPage(), "email", user1Email);
        testUtils.createUser(user2Login, "realuserpwd", testUtils.getURLToNonExistentPage(), "email", user2Email);
        testUtils.createUser(user3Login, "realuserpwd", testUtils.getURLToNonExistentPage(), "email", user3Email);

        testUtils.forceGuestUser();

        // check that when asking to retrieve username with a wrong email we don't get any information
        // if an user exists or not and no email is sent.
        ForgotUsernamePage forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail("notexistant@xwiki.com");
        ForgotUsernameCompletePage forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertTrue(forgotUsernameCompletePage.isForgotUsernameQuerySent());

        // we are waiting 5 sec here just to be sure no mail is sent, maybe we could decrease the timeout value,
        // not sure.
        assertFalse(this.mail.waitForIncomingEmail(1));

        // Bypass the check that prevents to reload the current page
        testUtils.gotoPage(testUtils.getURLToNonExistentPage());

        // test getting email for a forgot username request where the email is set in one account only
        forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail(user3Email);
        forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertTrue(forgotUsernameCompletePage.isForgotUsernameQuerySent());
        assertTrue(this.mail.waitForIncomingEmail(1));
        MimeMessage[] receivedEmails = this.mail.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        MimeMessage receivedEmail = receivedEmails[0];
        assertTrue(receivedEmail.getSubject().contains("Forgot username on"));
        String receivedMailContent = getMessageContent(receivedEmail).get("textPart");
        assertTrue(receivedMailContent.contains(String.format("XWiki.%s", user3Login)));

        // remove mails for last test
        this.mail.purgeEmailFromAllMailboxes();

        // Bypass the check that prevents to reload the current page
        testUtils.gotoPage(testUtils.getURLToNonExistentPage());

        // test getting email for a forgot username request where the email is set in two accounts
        forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail(user1Email);
        forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertTrue(forgotUsernameCompletePage.isForgotUsernameQuerySent());
        assertTrue(this.mail.waitForIncomingEmail(1));
        receivedEmails = this.mail.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        receivedEmail = receivedEmails[0];
        assertTrue(receivedEmail.getSubject().contains("Forgot username on"));
        receivedMailContent = getMessageContent(receivedEmail).get("textPart");
        assertTrue(receivedMailContent.contains(String.format("XWiki.%s", user1Login)));
        assertTrue(receivedMailContent.contains(String.format("XWiki.%s", user2Login)));
    }
}
