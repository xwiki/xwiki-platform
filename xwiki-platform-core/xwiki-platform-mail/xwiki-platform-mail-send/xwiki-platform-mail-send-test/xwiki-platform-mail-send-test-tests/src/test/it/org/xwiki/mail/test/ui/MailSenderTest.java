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

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

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
        getUtil().updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, "smtp_port", 3025);
        getUtil().updateObject("XWiki", "XWikiPreferences", "XWiki.XWikiPreferences", 0, "smtp_server", "localhost");

        // Create a Wiki page containing a Mail Template (ie a XWiki.Mail object)
        getUtil().createPage(getTestClassName(), "MailTemplate", "", "");
        getUtil().addObject(getTestClassName(), "MailTemplate", "XWiki.Mail",
            "subject", "Status for $name", "html", "<strong>Hello $name</strong>", "text", "Hello $name");

        // Create another page with the Velocity script to send the template email
        String velocity = "{{velocity}}\n"
            + "#set ($message = $services.mailsender.createMessage('localhost@xwiki.org', 'mary@doe.com', "
                + "'john@doe.com', 'subject test'))\n"
            + "#set ($templateReference = $services.model.createDocumentReference('', '" + getTestClassName()
                + "', 'MailTemplate'))\n"
            + "#set ($discard = $message.addPart('xwiki/template', $templateReference, "
                + "{'velocityVariables' : { 'name' : 'John' }}))\n"
            + "#set ($discard = $message.send())\n"
            + "{{/velocity}}";
        getUtil().createPage(getTestClassName(), "SendMail", velocity, "");

        // Verify that the mail has been received.
        this.mail.waitForIncomingEmail(10000L, 1);
        MimeMessage[] messages = this.mail.getReceivedMessages();

    }
}
