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
package org.xwiki.sharepage.test.ui;

import java.util.Arrays;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.sharepage.test.po.ShareDialog;
import org.xwiki.sharepage.test.po.ShareResultDialog;
import org.xwiki.sharepage.test.po.ShareableViewPage;
import org.xwiki.test.ui.AbstractTest;

import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * UI tests for the Share by Email application.
 *
 * @version $Id$
 * @since 7.0RC1
 */
public class SharePageTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private GreenMail mail;

    @Before
    public void startMail()
    {
        this.mail = new GreenMail(ServerSetupTest.SMTP);
        this.mail.start();
        getUtil().updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0, "host", "localhost", "port",
            "3025", "sendWaitTime", "0", "discardSuccessStatuses", "0");
    }

    @After
    public void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
        }
    }

    @Test
    public void testShareByEmail() throws Exception
    {
        // Delete existing test pages
        getUtil().deletePage(getTestClassName(), getTestMethodName());

        // Create a user to share with
        String userName = getTestClassName() + "_" + getTestMethodName();
        // Note: We need to set the first and last name as otherwise the suggest picker won't list the user
        // (it does an order by first name and last name).
        getUtil().createUser(userName, "pass", getUtil().getURLToNonExistentPage(),
            "first_name", "John", "last_name", "Doe", "email", "mail1@doe.com");

        // Create the page to share
        getUtil().createPage(getTestClassName(), getTestMethodName(), "something", "title");
        ShareableViewPage svp = new ShareableViewPage();

        // Share the page
        svp.clickShareByEmail();
        ShareDialog sd = new ShareDialog();
        sd.addUser("XWiki." + userName);
        sd.addEmail("mail2@doe.com");
        sd.setMessage("test");
        ShareResultDialog srd = sd.sendMail();

        assertEquals("The page is being sent asynchronously to all specified recipients.", srd.getResultMessage());
        srd.clickBackLink();

        // Verify we received the email and that its content is valid
        this.mail.waitForIncomingEmail(10000L, 2);

        MimeMessage[] messages = this.mail.getReceivedMessages();
        List<String> tos = Arrays.asList(messages[0].getHeader("to", ","), messages[1].getHeader("to", ","));
        assertThat(tos, hasItems("mail1@doe.com", "mail2@doe.com"));

        assertEquals("superadmin wants to share a document with you", messages[0].getSubject());
        assertEquals("superadmin wants to share a document with you", messages[1].getSubject());
    }
}
