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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;

/**
 * UI tests for the Mail Sender feature.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class MailSenderTest extends AbstractTest
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
    public void testMailSending() throws Exception
    {
        // Remove existing pages
        getUtil().deletePage(getTestClassName(), "MailTemplate");
        getUtil().deletePage(getTestClassName(), "SendMail");

        // Configure the SMTP host/port for the wiki so that it points to GreenMail.
        getUtil().addObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences");
        getUtil().updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, "smtp_port", 3025);
        getUtil().updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, "smtp_server", "localhost");

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
            + "{{/velocity}}";
        getUtil().createPage(getTestClassName(), "SendMail", velocity, "");

        // Verify that the mail has been received.
        this.mail.waitForIncomingEmail(10000L, 1);
        assertEquals(1, this.mail.getReceivedMessages().length);
        assertEquals("Status for John on " + getTestClassName() + ".SendMail",
            this.mail.getReceivedMessages()[0].getSubject());
    }
}
