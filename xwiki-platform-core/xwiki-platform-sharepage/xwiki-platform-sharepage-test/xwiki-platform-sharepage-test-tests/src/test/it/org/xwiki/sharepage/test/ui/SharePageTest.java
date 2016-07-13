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
import org.xwiki.test.ui.po.ViewPage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;

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
            "3025", "sendWaitTime", "0");
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
        // Delete any existing test page
        getUtil().deletePage(getTestClassName(), getTestMethodName());

        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), "something", "title");
        ShareableViewPage svp = new ShareableViewPage();
        svp.clickShareByEmail();
        ShareDialog sd = new ShareDialog();

        sd.setEmailField("john@doe.com");
        sd.setMessage("test");
        ShareResultDialog srd = sd.sendMail();
        assertEquals("The message has been sent to john.", srd.getResultMessage());
        srd.clickBackLink();

        // Verify we received the email and that its content is valid
        this.mail.waitForIncomingEmail(10000L, 1);

        MimeMessage mimeMessage = this.mail.getReceivedMessages()[0];
        assertEquals("superadmin wants to share a document with you", mimeMessage.getSubject());
    }
}
