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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.mail.test.po.MailStatusAdministrationSectionPage;
import org.xwiki.mail.test.po.SendMailAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Mail application.
 *
 * @version $Id$
 * @since 6.4M2
 */
@UITest(/*servletEngine = ServletEngine.TOMCAT,*/database = Database.MYSQL, verbose = true,
    sshPorts = {
        3025
    },
    properties = {
        "xwikiDbHbmCommonExtraMappings=mailsender.hbm.xml",
        // Pages created in the tests need to have PR since we ask for PR to send mails so we need to exclude them from
        // the PR checker.
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:MailIT\\..*"
    },
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-mail-send-storage"
    }
)
public class MailIT
{
    private GreenMail mail;

    private List<String> alreadyAssertedMessages = new ArrayList<>();

    private String testClassName;

    @BeforeEach
    public void setUp(TestInfo info)
    {
        this.testClassName = info.getTestClass().get().getSimpleName();
    }

    @BeforeEach
    public void startMail()
    {
        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
    }

    @AfterEach
    public void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
        }
    }

    @Test
    public void verifyMail(TestUtils setup, XWikiWebDriver webDriver, TestConfiguration testConfiguration)
        throws Exception
    {
        // Log in as superadmin
        setup.loginAsSuperAdmin();

        // Step 0: Delete all pre-existing mails to start clean. This also verifies the deleteAll() script service
        //         API.
        String content = "{{velocity}}$services.mailstorage.deleteAll(){{/velocity}}";
        ViewPage deleteAllPage = setup.createPage(this.testClassName, "DeleteAll", content, "");
        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", deleteAllPage.getContent());

        // Step 1: Verify that there are 2 email sections in the Mail category

        AdministrationPage wikiAdministrationPage = AdministrationPage.gotoPage();

        assertTrue(wikiAdministrationPage.hasSection("Mail", "Mail Sending"));
        assertTrue(wikiAdministrationPage.hasSection("Mail", "Mail Sending Status"));
        assertTrue(wikiAdministrationPage.hasSection("Mail", "Advanced"));

        // Verify we can click on Mail > Advanced
        wikiAdministrationPage.clickSection("Mail", "Advanced");

        // Step 2: Before validating that we can send email, let's verify that we can report errors when the mail
        // setup is not correct

        // Make sure there's an invalid mail server set.
        wikiAdministrationPage.clickSection("Mail", "Mail Sending");
        SendMailAdministrationSectionPage sendMailPage = new SendMailAdministrationSectionPage();
        sendMailPage.setHost("invalidmailserver");
        sendMailPage.clickSave();

        // Send the mail that's supposed to fail and validate that it fails
        sendMailWithInvalidMailSetup(setup);

        // Step 3: Navigate to each mail section and set the mail sending parameters (SMTP host/port)
        wikiAdministrationPage = AdministrationPage.gotoPage();
        wikiAdministrationPage.clickSection("Mail", "Mail Sending");
        sendMailPage = new SendMailAdministrationSectionPage();
        sendMailPage.setHost(testConfiguration.getServletEngine().getHostIP());
        sendMailPage.setPort("3025");
        // Make sure we don't wait between email sending in order to speed up the test (and not incur timeouts when
        // we wait to receive the mails)
        sendMailPage.setSendWaitTime("0");
        // Keep all mail statuses including successful ones (so that we verify this works fine)
        sendMailPage.setDiscardSuccessStatuses(false);
        sendMailPage.clickSave();

        // Step 3: Verify that there are no admin email sections when administering a space

        // Select XWiki space administration.
        AdministrationPage spaceAdministrationPage = AdministrationPage.gotoSpaceAdministrationPage("XWiki");

        // 2018-01-31: Got a failure on CI showing that the first assert below this line was failing because the
        // current page was still the one before move to the XWiki space admin. Thus taking extra step to ensure we
        // wait. However I don't understand why this happens since getDriver().url() called by
        // gotoSpaceAdministrationPage() should wait for the page to be loaded before returning.
        webDriver.waitUntilCondition(driver ->
            spaceAdministrationPage.getMetaDataValue("reference").equals("xwiki:XWiki.WebPreferences"));

        // All those sections should not be present
        assertTrue(spaceAdministrationPage.hasNotSection("Mail", "Mail Sending"));
        assertTrue(spaceAdministrationPage.hasNotSection("Mail", "Mail Sending Status"));
        assertTrue(spaceAdministrationPage.hasNotSection("Mail", "Advanced"));

        // Step 4: Prepare a Template Mail
        DocumentReference mailTemplateReference = new DocumentReference("xwiki", this.testClassName, "MailTemplate");
        setup.rest().delete(mailTemplateReference);

        // Create a Wiki page containing a Mail Template (ie a XWiki.Mail object)
        setup.createPage(mailTemplateReference, "", "");
        // Note: We use the following bindings in the Template subject and content so that we ensure that they are
        // provided by default:
        // - "$xwiki"
        // - "$xcontext"
        // - "$escapetool"
        // - "$services"
        // - "$request"
        // Note: We also use the $name and $doc bindings to show that the user can add new bindings ($doc is not bound
        // by default since there isn't always a notion of current doc in all places where mail sending is done).
        // Note: We use $xwiki.getURL() in the content to verify that we generate full external URLs.
        String velocityContent = "Hello $name from $escapetool.xml($services.model.resolveDocument("
            + "$xcontext.getUser()).getName()) - Served from $request.getRequestURL().toString() - "
            + "url: $xwiki.getURL('Main.WebHome')";
        setup.addObject(this.testClassName, "MailTemplate", "XWiki.Mail",
            "subject", "#if ($xwiki.exists($doc.documentReference))Status for $name on $doc.fullName#{else}wrong#end",
            "language", "en",
            "html", "<strong>" + velocityContent + "</strong>",
            "text", velocityContent);
        // We also add an attachment to the Mail Template page to verify that it is sent in the mail
        ByteArrayInputStream bais = new ByteArrayInputStream("Content of attachment".getBytes());
        setup.attachFile(this.testClassName, "MailTemplate", "something.txt", bais, true,
            new UsernamePasswordCredentials("superadmin", "pass"));

        String xwikiURLPrefix = String.format("http://%s:%s/xwiki/bin/view",
            testConfiguration.getServletEngine().getInternalIP(),
            testConfiguration.getServletEngine().getInternalPort());

        // Step 5: Send a template email (with an attachment) to a single email address
        sendTemplateMailToEmail(setup, xwikiURLPrefix);

        // Step 6: Send a template email to all the users in the XWikiAllGroup Group (we'll create 2 users) + to
        // two other users (however since they're part of the group they'll receive only one mail each, we thus test
        // deduplicatio!).
        sendTemplateMailToUsersAndGroup(setup, xwikiURLPrefix);

        // Step 7: Navigate to the Mail Sending Status Admin page and assert that the Livetable displays the entry for
        // the sent mails
        wikiAdministrationPage = AdministrationPage.gotoPage();
        wikiAdministrationPage.clickSection("Mail", "Mail Sending Status");
        MailStatusAdministrationSectionPage statusPage = new MailStatusAdministrationSectionPage();
        LiveTableElement liveTableElement = statusPage.getLiveTable();
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-3", "Test");
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-5", "send_success");
        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-6", "xwiki");

        // Let's wait till we have at least 3 rows. Note that we wait because we could have received the mails above
        // but the last mail's status in the database may not have been updated yet. Note that The first 2 are
        // guaranteed to have been updated since we send mail in one thread one after another and we update the
        // database after sending each mail.
        liveTableElement.waitUntilRowCountGreaterThan(3);

        liveTableElement.filterColumn("xwiki-livetable-sendmailstatus-filter-4", "john@doe.com");
        assertTrue(liveTableElement.getRowCount() > 0);
        assertTrue(liveTableElement.hasRow("Error", ""));
    }

    private void sendMailWithInvalidMailSetup(TestUtils setup)
    {
        // Remove existing pages (for pages that we create below)
        setup.deletePage(this.testClassName, "SendInvalidMail");

        // Create a page with the Velocity script to send the template email.
        // Note that we don't set the type and thus this message should not appear in the LiveTable filter at the end
        // of the test.
        String velocity = "{{velocity}}\n"
            + "#set ($message = $services.mailsender.createMessage('from@doe.com', 'to@doe.com', 'Subject'))\n"
            + "#set ($discard = $message.addPart('text/plain', 'text message'))\n"
            + "#set ($result = $services.mailsender.send([$message], 'database'))\n"
            + "#foreach ($status in $result.statusResult.getAllErrors())\n"
            + "  MSGID $status.messageId SUMMARY $status.errorSummary DESCRIPTION $status.errorDescription\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = setup.createPage(this.testClassName, "SendInvalidMail", velocity, "");

        // Verify that the page is not empty (and thus an error message is displayed). Note that it's difficult to
        // assert what is displayed because it could vary from system to system. This is why we only assert that
        // something is displayed and that it matches the defined pattern.
        assertTrue(vp.getContent().matches("(?s)MSGID.*SUMMARY.*DESCRIPTION.*"));
    }

    private void sendTemplateMailToEmail(TestUtils setup, String xwikiURLPrefix) throws Exception
    {
        // Remove existing pages (for pages that we create below)
        setup.deletePage(this.testClassName, "SendMail");

        // Create another page with the Velocity script to send the template email
        // Note that we didn't need to bind the "$doc" velocity variable because the send is done synchronously and
        // thus the current XWiki Context is cloned before being passed to the template evaluation, and thus it
        // already contains the "$doc" binding!
        String velocity = "{{velocity}}\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + this.testClassName
                + "', 'MailTemplate'))\n"
            + "#set ($parameters = {'velocityVariables' : { 'name' : 'John' }, 'language' : 'en', "
                + "'includeTemplateAttachments' : true})\n"
            + "#set ($message = $services.mailsender.createMessage('template', $templateReference, $parameters))\n"
            + "#set ($discard = $message.setFrom('localhost@xwiki.org'))\n"
            + "#set ($discard = $message.addRecipients('to', 'john@doe.com'))\n"
            + "#set ($discard = $message.setType('Test'))\n"
            + "#set ($result = $services.mailsender.send([$message], 'database'))\n"
            + "#if ($services.mailsender.lastError)\n"
            + "  {{error}}$exceptiontool.getStackTrace($services.mailsender.lastError){{/error}}\n"
            + "#end\n"
            + "#foreach ($status in $result.statusResult.getByState('SEND_ERROR'))\n"
            + "  {{error}}\n"
            + "    $status.messageId - $status.errorSummary\n"
            + "    $status.errorDescription\n"
            + "  {{/error}}\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = setup.createPage(this.testClassName, "SendMail", velocity, "");

        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", vp.getContent());

        // Verify that the mail has been received.
        this.mail.waitForIncomingEmail(30000L, 1);
        assertEquals(1, this.mail.getReceivedMessages().length);
        assertReceivedMessages(1,
            "Subject: Status for John on " + this.testClassName + ".SendMail",
            "Hello John from superadmin - Served from " + xwikiURLPrefix + "/MailIT/SendMail",
            "<strong>Hello John from superadmin - Served from " + xwikiURLPrefix + "/MailIT/SendMail - "
                + "url: " + xwikiURLPrefix + "/Main/</strong>",
            "X-MailType: Test",
            "Content-Type: text/plain; name=something.txt",
            "Content-ID: <something.txt>",
            "Content-Disposition: attachment; filename=something.txt",
            "Content of attachment");
    }

    private void sendTemplateMailToUsersAndGroup(TestUtils setup, String xwikiURLPrefix) throws Exception
    {
        // Remove existing pages (for pages that we create below)
        setup.deletePage(this.testClassName, "SendMailGroupAndUsers");

        // Create 2 users
        setup.createUser("user1", "password1", setup.getURLToNonExistentPage(), "email", "user1@doe.com");
        setup.createUser("user2", "password2", setup.getURLToNonExistentPage(), "email", "user2@doe.com");

        // Create another page with the Velocity script to send the template email
        // Note: the $xcontext and $request bindings are present and have their values at the moment the call to send
        // the mail asynchronously was done.
        String velocity = "{{velocity}}\n"
            + "#set ($templateParameters = "
            + "  {'velocityVariables' : { 'name' : 'John', 'doc' : $doc }, "
            + "  'language' : 'en', 'from' : 'localhost@xwiki.org'})\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + this.testClassName
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
            + "#foreach ($status in $result.statusResult.getByState('SEND_ERROR'))\n"
            + "  {{error}}\n"
            + "    $status.messageId - $status.errorSummary\n"
            + "    $status.errorDescription\n"
            + "  {{/error}}\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = setup.createPage(testClassName, "SendMailGroupAndUsers", velocity, "");

        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", vp.getContent());

        // Verify that the mails have been received (first mail above + the 2 mails sent to the group)
        this.mail.waitForIncomingEmail(30000L, 3);
        assertEquals(3, this.mail.getReceivedMessages().length);
        assertReceivedMessages(2,
            "Subject: Status for John on " + this.testClassName + ".SendMailGroupAndUsers",
            "Hello John from superadmin - Served from " + xwikiURLPrefix + "/MailIT/SendMailGroupAndUsers - "
                + "url: " + xwikiURLPrefix + "/Main/");
    }

    private void assertReceivedMessages(int expectedMatchingCount, String... expectedLines) throws Exception
    {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (MimeMessage message : this.mail.getReceivedMessages()) {
            if (this.alreadyAssertedMessages.contains(message.getMessageID())) {
                continue;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            String fullContent = baos.toString();
            boolean match = true;
            for (int i = 0; i < expectedLines.length; i++) {
                if (!fullContent.contains(expectedLines[i])) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                builder.append("- Content [" + fullContent + "]").append('\n');
            } else {
                count++;
            }
            this.alreadyAssertedMessages.add(message.getMessageID());
        }
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < expectedLines.length; i++) {
            expected.append("- '" + expectedLines[i] + "'").append('\n');
        }
        assertEquals(expectedMatchingCount, count,
            String.format("We got [%s] mails matching the expected content instead of [%s]. We were expecting "
                + "the following content:\n%s\nWe got the following:\n%s", count, expectedMatchingCount,
            expected.toString(), builder.toString()));
    }
}
