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
package org.xwiki.administration.test.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.ResetPasswordCompletePage;
import org.xwiki.administration.test.po.ResetPasswordPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LoginPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the Reset Password feature.
 *
 * @version $Id$
 * @since 7.0M2
 */
@UITest(sshPorts = {
    // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
    // by GreenMail running on the host.
    3025
},
    properties = {
        // The Mail module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml",
        // Pages created in the tests need to have PR since we ask for PR to send mails so we need to exclude them from
        // the PR checker.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.ResetPassword|.*:XWiki\\.ResetPasswordComplete"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-mail-send-storage"
    }
)
public class ResetPasswordIT
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

    @Test
    public void resetForgottenPassword(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        String userName = "testUser" + RandomStringUtils.randomAlphanumeric(6);
        String password = "password";
        String newPassword = "newPassword";

        // Create a user
        setup.createUser(userName, password, null);

        // Make sure we are not logged in and go to the reset password page
        setup.forceGuestUser();
        ResetPasswordPage resetPasswordPage = ResetPasswordPage.gotoPage();

        // Try to reset the password of a non existent user
        resetPasswordPage.setUserName("SomeUserThatDoesNotExist");
        resetPasswordPage = resetPasswordPage.clickResetPassword();
        assertFalse(resetPasswordPage.isResetPasswordSent());
        assertTrue(resetPasswordPage.getMessage().contains("user does not exist"));

        // Try again
        resetPasswordPage = resetPasswordPage.clickRetry();

        // Try to reset the password of our user, when he has no email set
        resetPasswordPage.setUserName(userName);
        resetPasswordPage.clickResetPassword();
        assertFalse(resetPasswordPage.isResetPasswordSent());
        assertTrue(resetPasswordPage.getMessage().contains("email address not provided"));

        // Try again. This time, set the user's email address in the profile
        setup.loginAsSuperAdmin();
        setup.updateObject("XWiki", userName, "XWiki.XWikiUsers", 0, "email", "foo@bar.com", "form_token",
            setup.getSecretToken());
        setup.forceGuestUser();

        // Actually reset the user's password
        resetPasswordPage = ResetPasswordPage.gotoPage();
        resetPasswordPage.setUserName(userName);
        ResetPasswordPage newResetPasswordPage = resetPasswordPage.clickResetPassword();
        assertTrue(newResetPasswordPage.getMessage().contains("An e-mail was sent"),
            "Actual message: " + newResetPasswordPage.getMessage());
        assertFalse(newResetPasswordPage.getMessage().contains("foo@bar.com"),
            "Actual message: " + newResetPasswordPage.getMessage());

        // Check the result
        assertTrue(resetPasswordPage.isResetPasswordSent());
        // Check the emails received by the user
        assertTrue(this.mail.waitForIncomingEmail(1));
        MimeMessage[] receivedEmails = this.mail.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        MimeMessage receivedEmail = receivedEmails[0];
        assertEquals("Password reset request for " + userName, receivedEmail.getSubject());
        String receivedMailContent = getMessageContent(receivedEmail).get("textPart");
        String passwordResetLink = getResetLink(receivedMailContent, "xwiki%3AXWiki." + userName);
        assertNotNull(passwordResetLink);

        // Use the password reset link
        setup.gotoPage(passwordResetLink);
        // We should now be on the ResetPasswordComplete page
        ResetPasswordCompletePage resetPasswordCompletePage = new ResetPasswordCompletePage();
        // Check that the link was valid
        assertTrue(resetPasswordCompletePage.isResetLinkValid());
        resetPasswordCompletePage.setPassword(newPassword);
        resetPasswordCompletePage.setPasswordConfirmation(newPassword);
        resetPasswordCompletePage = resetPasswordCompletePage.clickSave();

        // Check the result
        assertTrue(resetPasswordCompletePage.isPasswordSuccessfullyReset());
        LoginPage loginPage = resetPasswordCompletePage.clickLogin();

        // Check the new password
        loginPage.loginAs(userName, newPassword);
        assertEquals(userName, setup.getLoggedInUserName());
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

    private String getResetLink(String emailContent, String userName)
    {
        String result = null;

        // Use a regex to extract the password reset link
        Pattern resetLinkPattern = Pattern.compile("http[^\\s]+?authenticate/reset\\?u=" + userName + "\\&v=\\w+");
        Matcher matcher = resetLinkPattern.matcher(emailContent);
        if (matcher.find()) {
            result = matcher.group();
        }

        return result;
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
}
