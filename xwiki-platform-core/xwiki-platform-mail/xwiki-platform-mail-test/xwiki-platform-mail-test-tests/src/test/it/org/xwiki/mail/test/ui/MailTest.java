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

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrablePage;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;

/**
 * UI tests for the Mail application.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class MailTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil(), getDriver());

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

        // Step 2: Navigate to each mail section

        administrationPage.clickSection("Email", "General");
        administrationPage.clickSection("Email", "Mail Sending");

        // Step 3: verify that there are no admin email sections when administering a space

        // Select XWiki space administration.
        AdministrationPage spaceAdministrationPage = administrationPage.selectSpaceToAdminister("XWiki");

        // Since clicking on "XWiki" in the Select box will reload the page asynchronously we need to wait for the new
        // page to be available. For this we wait for the heading to be changed to "Administration:XWiki".
        spaceAdministrationPage.waitUntilElementIsVisible(By.id("HAdministration:XWiki"));
        // Also wait till the page is fully loaded to be extra sure...
        spaceAdministrationPage.waitUntilPageIsLoaded();

        // All those sections should not be present
        Assert.assertTrue(spaceAdministrationPage.hasNotSection("Email", "General"));
        Assert.assertTrue(spaceAdministrationPage.hasNotSection("Email", "Mail Sending"));

        // Step 4: Try sending an email

        // Remove existing pages (for pages that we create below)
        getUtil().deletePage(getTestClassName(), "MailTemplate");
        getUtil().deletePage(getTestClassName(), "SendMail");

        // Configure the SMTP host/port for the wiki so that it points to GreenMail.
        // TODO: Replace this by user actions on the UI to prove that it works...
        getUtil().updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "port", 3025);
        getUtil().updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host", "localhost");

        // Create a Wiki page containing a Mail Template (ie a XWiki.Mail object)
        getUtil().createPage(getTestClassName(), "MailTemplate", "", "");
        // Note: we use the $doc binding in the content to verify that standard variables are correctly bound.
        getUtil().addObject(getTestClassName(), "MailTemplate", "XWiki.Mail",
            "subject", "Status for $name on $doc.fullName", "language", "en", "html", "<strong>Hello $name</strong>",
            "text", "Hello $name");
        ByteArrayInputStream bais = new ByteArrayInputStream("content".getBytes());
        getUtil().attachFile(getTestClassName(), "MailTemplate", "something.txt", bais, true,
            new UsernamePasswordCredentials("superadmin", "pass"));

        // Create another page with the Velocity script to send the template email
        String velocity = "{{velocity}}\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + getTestClassName()
            + "', 'MailTemplate'))\n"
            + "#set ($parameters = {'velocityVariables' : { 'name' : 'John' }, 'language' : 'en'})\n"
            + "#set ($message = $services.mailsender.createMessage('template', $templateReference, $parameters))\n"
            + "#set ($discard = $message.setFrom('localhost@xwiki.org'))\n"
            + "#set ($discard = $message.addRecipients('to', 'john@doe.com'))\n"
            + "#set ($discard = $message.send())\n"
            + "#if ($services.mailsender.lastError)\n"
            + "  {{error}}$exceptiontool.getStackTrace($services.mailsender.lastError){{/error}}\n"
            + "#end\n"
            + "{{/velocity}}";
        // This will create the page and execute its content and thus send the mail
        ViewPage vp = getUtil().createPage(getTestClassName(), "SendMail", velocity, "");

        // Verify that the page doesn't display any content (unless there's an error!)
        assertEquals("", vp.getContent());

        // Verify that the mail has been received.
        this.mail.waitForIncomingEmail(10000L, 1);
        assertEquals(1, this.mail.getReceivedMessages().length);
        assertEquals("Status for John on " + getTestClassName() + ".SendMail",
            this.mail.getReceivedMessages()[0].getSubject());
    }
}
