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
package org.xwiki.mail.integration;

import java.io.ByteArrayOutputStream;

import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.internal.DefaultMailSender;
import org.xwiki.mail.script.MailSenderScriptService;
import org.xwiki.mail.script.ScriptMimeMessage;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.AllComponents;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertTrue;

/**
 * Integration tests to prove that mail sending is working fully end to end with the Scripting API.
 *
 * @version $Id$
 * @since 6.1M2
 */
@AllComponents
public class ScriptingIntegrationTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    private GreenMail mail;

    private MailSenderScriptService scriptService;

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

    @Before
    public void initialize() throws Exception
    {
        this.scriptService = this.componentManager.getInstance(ScriptService.class, "mailsender");
    }

    @After
    public void cleanUp() throws Exception
    {
        // Make sure we stop the Mail Sender thread after each test (since it's started automatically when looking
        // up the MailSender component.
        ((DefaultMailSender) this.componentManager.getInstance(MailSender.class)).stopMailSenderThread();
    }

    @Test
    public void sendMail() throws Exception
    {
        ScriptMimeMessage message = this.scriptService.createMessage("john@doe.com", "subject");
        message.addPart("plain/text", "some text here");

        // Send 3 mails (3 times the same mail)
        message.send();
        message.send();
        message.send();

        // Verify that the mails have been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 3);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        messages[0].writeTo(baos);
        String messageText = baos.toString();

        assertTrue(messageText.contains("To: john@doe.com"));
        assertTrue(messageText.contains("Subject: subject"));
        assertTrue(messageText.contains("Content-Type: multipart/mixed"));
        assertTrue(messageText.contains("Content-Type: text/plain"));
        assertTrue(messageText.contains("some text here"));
    }
}
