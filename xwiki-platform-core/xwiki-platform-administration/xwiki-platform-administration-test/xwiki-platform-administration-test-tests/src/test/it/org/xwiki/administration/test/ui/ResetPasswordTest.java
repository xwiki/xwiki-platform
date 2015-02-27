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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.ResetPasswordCompletePage;
import org.xwiki.administration.test.po.ResetPasswordPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ViewPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Verify the Reset Password feature.
 *
 * @version $Id$
 * @since 7.0M2
 */
public class ResetPasswordTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private GreenMail mail;

    @Before
    public void startMail()
    {
        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();

        configureEmail();
    }

    @After
    public void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
        }

        restoreSettings();
    }

    private void configureEmail()
    {
        getUtil().updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host", "localhost", "port",
            "3025", "sendWaitTime", "0");
    }

    private void restoreSettings()
    {
        // Make sure we can restore the settings, so we log back with superadmin to finish the work
        getUtil().loginAsSuperAdmin();

        // Remove the previous version that the setup has created.
        getUtil().deleteLatestVersion("Mail", "MailConfig");
    }

    @Test
    public void resetForgottenPassword() throws Exception
    {
        String userName = "testUser" + RandomStringUtils.randomAlphanumeric(6);
        String password = "password";
        String newPassword = "newPassword";

        // Create a user
        getUtil().createUser(userName, password, null);

        // Make sure we are not logged in and go to the reset password page
        getUtil().forceGuestUser();
        ResetPasswordPage resetPasswordPage = ResetPasswordPage.gotoPage();

        // Try to reset the password of a non existent user
        resetPasswordPage.setUserName("SomeUserThatDoesNotExist");
        resetPasswordPage.clickResetPassword();
        Assert.assertFalse(resetPasswordPage.isResetPasswordSent());
        Assert.assertTrue(resetPasswordPage.getMessage().contains("user does not exist"));

        // Try again
        resetPasswordPage = resetPasswordPage.clickRetry();

        // Try to reset the password of our user, when he has no email set
        resetPasswordPage.setUserName(userName);
        resetPasswordPage.clickResetPassword();
        Assert.assertFalse(resetPasswordPage.isResetPasswordSent());
        Assert.assertTrue(resetPasswordPage.getMessage().contains("email address not provided"));

        // Try again. This time, set the user's email address in the profile
        getUtil().loginAsSuperAdmin();
        getUtil().updateObject("XWiki", userName, "XWiki.XWikiUsers", 0, "email", "foo@bar.com", "form_token",
            getUtil().getSecretToken());
        new ViewPage().logout();

        // Actually reset the user's password
        resetPasswordPage = ResetPasswordPage.gotoPage();
        resetPasswordPage.setUserName(userName);
        resetPasswordPage.clickResetPassword();

        // Check the result
        Assert.assertTrue(resetPasswordPage.isResetPasswordSent());
        // Check the emails received by the user
        MimeMessage[] receivedEmails = this.mail.getReceivedMessages();
        Assert.assertEquals(1, receivedEmails.length);
        MimeMessage receivedEmail = receivedEmails[0];
        Assert.assertEquals("Password reset request for " + userName, receivedEmail.getSubject());
        String receivedMailContent = getMessageContent(receivedEmail).get("textPart");
        String passwordResetLink = getResetLink(receivedMailContent, userName);
        Assert.assertNotNull(passwordResetLink);

        // Use the password reset link
        getUtil().gotoPage(passwordResetLink);
        // We should now be on the ResetPasswordComplete page
        ResetPasswordCompletePage resetPasswordCompletePage = new ResetPasswordCompletePage();
        // Check that the link was valid
        Assert.assertTrue(resetPasswordCompletePage.isResetLinkValid());
        resetPasswordCompletePage.setPassword(newPassword);
        resetPasswordCompletePage.setPasswordConfirmation(newPassword);
        resetPasswordCompletePage = resetPasswordCompletePage.clickSave();

        // Check the result
        Assert.assertTrue(resetPasswordCompletePage.isPasswordSuccessfullyReset());
        LoginPage loginPage = resetPasswordCompletePage.clickLogin();

        // Check the new password
        loginPage.loginAs(userName, newPassword);
        Assert.assertEquals(userName, getUtil().getLoggedInUserName());
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
            messageMap.put("textPart", IOUtils.toString(plain.getInputStream()));
        }
        BodyPart html = getPart(mp, "text/html");
        if (html != null) {
            messageMap.put("htmlPart", IOUtils.toString(html.getInputStream()));
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

            if (part.isMimeType("multipart/related") || part.isMimeType("multipart/alternative")
                || part.isMimeType("multipart/mixed")) {
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
        Pattern resetLinkPattern = Pattern.compile("http[^\\s]+?ResetPasswordComplete\\?u=" + userName + "\\&v=\\w+");
        Matcher matcher = resetLinkPattern.matcher(emailContent);
        if (matcher.find()) {
            result = matcher.group();
        }

        return result;
    }
}
