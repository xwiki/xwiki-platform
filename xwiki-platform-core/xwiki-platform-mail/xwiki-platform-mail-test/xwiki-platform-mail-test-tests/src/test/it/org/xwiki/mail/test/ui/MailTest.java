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
package org.xwiki.mail.test.ui;

import java.io.ByteArrayInputStream;

import javax.mail.internet.MimeMessage;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.mail.test.po.MailStatusAdministrationSectionPage;
import org.xwiki.mail.test.po.SendMailAdministrationSectionPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.*;

/**
 * UI tests for the Mail application.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class MailTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private GreenMail mail;

    @Before
    public void startMail()
    {
        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    @After
    public void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
        }
    }

    @Test
    public void testMail() throws Exception
    {
        // Because of http://jira.xwiki.org/browse/XWIKI-9763 we need to create a test page to ensure there's at least
        // one non-hidden page in the XWiki space
        // TODO: Remove this once http://jira.xwiki.org/browse/XWIKI-9763 is fixed.
        getUtil().createPage("XWiki", getTestClassName() + "-" + getTestMethodName(), "", "");

        // Step 1: Verify that there are 2 email sections in the Email category

        AdministrablePage page = new AdministrablePage();
        AdministrationPage administrationPage = page.clickAdministerWiki();

        Assert.assertTrue(administrationPage.hasSection("Email", "General"));
        Assert.assertTrue(administrationPage.hasSection("Email", "Mail Sending"));

        // Step 2: Navigate to each mail section and set the mail sending parameters (SMTP host/port)

        administrationPage.clickSection("Email", "General");
        administrationPage.clickSection("Email", "Mail Sending");
        SendMailAdministrationSectionPage sendMailPage = new SendMailAdministrationSectionPage();
        sendMailPage.setHost("localhost");
        sendMailPage.setPort("3025");
        // Make sure we don't wait between email sending in order to speed up the test (and not incur timeouts when
        // we wait to receive the mails)
        sendMailPage.setSendWaitTime("0");
        // Keep all mail statuses including successful ones (so that we verify this works fine)
        sendMailPage.setDiscardSuccessStatuses(false);
        sendMailPage.clickSave();

        // Step 3: Verify that there are no admin email sections when administering a space

        // Select XWiki space administration.
        AdministrationPage spaceAdministrationPage = administrationPage.selectSpaceToAdminister("XWiki");

        // Since clicking on "XWiki" in the Select box will reload the page asynchronously we need to wait for the new
        // page to be available. For this we wait for the heading to be changed to "Administration:XWiki".
        getDriver().waitUntilElementIsVisible(By.id("HAdministration:XWiki"));
        // Also wait till the page is fully loaded to be extra sure...
        spaceAdministrationPage.waitUntilPageIsLoaded();

        // All those sections should not be present
        Assert.assertTrue(spaceAdministrationPage.hasNotSection("Email", "General"));
        Assert.assertTrue(spaceAdministrationPage.hasNotSection("Email", "Mail Sending"));

        // Step 4: Prepare a Template Mail
        getUtil().deletePage(getTestClassName(), "MailTemplate");

        // Create a Wiki page containing a Mail Template (ie a XWiki.Mail object)
        getUtil().createPage(getTestClassName(), "MailTemplate", "", "");
        // Note: we use the $xwiki binding in the content to verify that standard variables are correctly bound.
        // Note: we use the $doc binding to show that the user can add new bindings ($doc is not bound by default).
        getUtil().addObject(getTestClassName(), "MailTemplate", "XWiki.Mail",
            "subject", "#if ($xwiki.exists($doc.documentReference))Status for $name on $doc.fullName#{else}wrong#end",
            "language", "en",
            "html", "<strong>Hello $name</strong>",
            "text", "Hello $name");
        ByteArrayInputStream bais = new ByteArrayInputStream("content".getBytes());
        getUtil().attachFile(getTestClassName(), "MailTemplate", "something.txt", bais, true,
            new UsernamePasswordCredentials("superadmin", "pass"));

        // Step 5: Send a template email (with an attachment) to a single email address
        sendTemplateMailToEmail();

        // Step 6: Send a template email to all the users in the XWikiAllGroup Group (we'll create 2 users) + to
        // two other users (however since they're part of the group they'll receive only one mail each).
        sendTemplateMailToUsersAndGroup();

        // Step 7: Navigate to the Mail Sending Status Admin page and assert that the Livetable displays the entry for
        // the sent mails
        administrationPage = AdministrationPage.gotoPage();
        administrationPage.clickSection("Email", "Mail Sending Status");
        MailStatusAdministrationSectionPage statusPage = new MailStatusAdministrationSectionPage();
        LiveTableElement liveTableElement = statusPage.getLiveTable();
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-3", "Test");
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-5", "sent");
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-6", "xwiki");
        assertTrue(liveTableElement.getRowCount() > 2);
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-4", "john@doe.com");
        assertTrue(liveTableElement.getRowCount() > 0);
        assertTrue(liveTableElement.hasRow("Error", ""));
    }

    private void sendTemplateMailToEmail() throws Exception
    {
        // Remove existing pages (for pages that we create below)
        getUtil().deletePage(getTestClassName(), "SendMail");

        // Create another page with the Velocity script to send the template email
        String velocity = "{{velocity}}\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + getTestClassName()
            + "', 'MailTemplate'))\n"
            + "#set ($parameters = {'velocityVariables' : { 'name' : 'John' }, 'language' : 'en'})\n"
            + "#set ($message = $services.mailsender.createMessage('template', $templateReference, $parameters))\n"
            + "#set ($discard = $message.setFrom('localhost@xwiki.org'))\n"
            + "#set ($discard = $message.addRecipients('to', 'john@doe.com'))\n"
            + "#set ($discard = $message.setType('Test'))\n"
            + "#set ($result = $services.mailsender.send([$message], 'database'))\n"
            + "#if ($services.mailsender.lastError)\n"
            + "  {{error}}$exceptiontool.getStackTrace($services.mailsender.lastError){{/error}}\n"
            + "#end\n"
            + "#foreach ($status in $result.statusResult.getByState('FAILED'))\n"
            + "  {{error}}\n"
            + "    $status.messageId - $status.errorSummary\n"
            + "    $status.errorDescription\n"
            + "  {{/error}}\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = getUtil().createPage(getTestClassName(), "SendMail", velocity, "");

        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", vp.getContent());

        // Verify that the mail has been received.
        this.mail.waitForIncomingEmail(10000L, 1);
        assertEquals(1, this.mail.getReceivedMessages().length);
        assertNumberOfReceivedMessagesWithSubject(1, "Status for John on " + getTestClassName() + ".SendMail");
    }

    private void sendTemplateMailToUsersAndGroup() throws Exception
    {
        // Remove existing pages (for pages that we create below)
        getUtil().deletePage(getTestClassName(), "SendMailGroupAndUsers");

        // Create 2 users
        getUtil().createUser("user1", "password1", getUtil().getURLToNonExistentPage(), "email", "user1@doe.com");
        getUtil().createUser("user2", "password2", getUtil().getURLToNonExistentPage(), "email", "user2@doe.com");

        // Create another page with the Velocity script to send the template email
        String velocity = "{{velocity}}\n"
            + "#set ($templateParameters = {'velocityVariables' : { 'name' : 'John', 'doc' : $doc }, "
            + "'language' : 'en', 'from' : 'localhost@xwiki.org'})\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + getTestClassName()
            + "', 'MailTemplate'))\n"
            + "#set ($parameters = {'hint' : 'template', 'source' : $templateReference, "
            + "'parameters' : $templateParameters, 'type' : 'Test'})\n"
            + "#set ($groupReference = $services.model.createDocumentReference('', 'XWiki', 'XWikiAllGroup'))\n"
            + "#set ($user1Reference = $services.model.createDocumentReference('', 'XWiki', 'user1'))\n"
            + "#set ($user2Reference = $services.model.createDocumentReference('', 'XWiki', 'user2'))\n"
            + "#set ($source = {'groups' : [$groupReference], 'users' : [$user1Reference, $user2Reference]})\n"
            + "#set ($messages = $services.mailsender.createMessages('usersandgroups', $source, $parameters))\n"
            + "#set ($result = $services.mailsender.send($messages, 'database'))\n"
            + "#if ($services.mailsender.lastError)\n"
            + "  {{error}}$exceptiontool.getStackTrace($services.mailsender.lastError){{/error}}\n"
            + "#end\n"
            + "#foreach ($status in $result.statusResult.getByState('FAILED'))\n"
            + "  {{error}}\n"
            + "    $status.messageId - $status.errorSummary\n"
            + "    $status.errorDescription\n"
            + "  {{/error}}\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = getUtil().createPage(getTestClassName(), "SendMailGroupAndUsers", velocity, "");

        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", vp.getContent());

        // Verify that the mails have been received (first mail above + the 2 mails sent to the group)
        this.mail.waitForIncomingEmail(10000L, 3);
        assertEquals(3, this.mail.getReceivedMessages().length);
        assertNumberOfReceivedMessagesWithSubject(2,
            "Status for John on " + getTestClassName() + ".SendMailGroupAndUsers");
    }

    private void assertNumberOfReceivedMessagesWithSubject(int nb, String subject) throws Exception
    {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (MimeMessage message : this.mail.getReceivedMessages()) {
            builder.append('[').append(message.getSubject()).append(']').append('\n');
            if (message.getSubject().equals(subject)) {
                count++;
            }
        }
        assertEquals("We got the following subjects instead of the required " + nb + " for [" + subject + "]:\n"
            + builder.toString(), nb, count);
    }
}
