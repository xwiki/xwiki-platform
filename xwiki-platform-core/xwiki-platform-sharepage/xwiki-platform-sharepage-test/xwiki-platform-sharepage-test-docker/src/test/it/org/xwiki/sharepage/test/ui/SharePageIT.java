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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.sharepage.test.po.ShareDialog;
import org.xwiki.sharepage.test.po.ShareResultDialog;
import org.xwiki.sharepage.test.po.ShareableViewPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UI tests for the Share by Email application.
 *
 * @version $Id$
 * @since 7.0RC1
 */
@UITest(
    sshPorts = {
        // Open the GreenMail port so that the XWiki instance inside a Docker container can use the SMTP server provided
        // by GreenMail running on the host.
        3025
    },
    properties = {
        // Add the MailSender plugin used by the SharePage UI to send mails
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.mailsender.MailSenderPlugin"
    },
    extraJARs = {
        // XWiki needs the mailsender plugin JAR to be present before it starts since it's not an extension and it
        // cannot be provisioned after XWiki is started!
        "org.xwiki.platform:xwiki-platform-mailsender",
        // MailSender plugin uses MailSenderConfiguration from xwiki-platform-mail-api so we need to provide an
        // implementation for it.
        "org.xwiki.platform:xwiki-platform-mail-send-default"
    }
)
public class SharePageIT
{
    @RegisterExtension
    private static GreenMailExtension mail = new GreenMailExtension(ServerSetupTest.SMTP);

    private String testClassName;

    private String testMethodName;

    @BeforeEach
    public void setup(TestUtils setup, TestInfo info)
    {
        this.testClassName = info.getTestClass().get().getSimpleName();
        this.testMethodName = info.getTestMethod().get().getName();
        this.mail.reset();

        setup.loginAsSuperAdmin();

        // Delete any existing test page
        setup.deletePage(this.testClassName, this.testMethodName);
    }

    @Test
    @Order(1)
    public void shareByEmailWhenFromAddressSpecified(TestUtils setup, TestConfiguration configuration) throws Exception
    {
        setup.updateObject("Mail", "MailConfig", "Mail.SendMailConfigClass", 0,
            "host", configuration.getServletEngine().getHostIP(),
            "port", "3025",
            "sendWaitTime", "0",
            "from", "noreply@localhost");
        shareByEmail("noreply@localhost", setup);
    }

    private void shareByEmail(String expectedEmail, TestUtils setup) throws Exception
    {
        setup.createPage(this.testClassName, this.testMethodName, "something", "title");
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

        assertEquals(expectedEmail, mimeMessage.getFrom()[0].toString());
    }
}
