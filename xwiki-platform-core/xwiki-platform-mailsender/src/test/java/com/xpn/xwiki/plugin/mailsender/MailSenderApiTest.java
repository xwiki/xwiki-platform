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
package com.xpn.xwiki.plugin.mailsender;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link com.xpn.xwiki.plugin.mailsender.Mail}.
 */
@OldcoreTest
public class MailSenderApiTest
{
    @MockComponent
    private MailSenderConfiguration mockConfiguration;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private MailSenderPluginApi api;

    private static GreenMail mailserver;

    @BeforeAll
    public static void beforeAll()
    {
        // Increase startup timeout (default is 1s, which can be too fast on slow CI agents).
        ServerSetup newSetup = ServerSetupTest.SMTP.createCopy();
        newSetup.setServerStartupTimeout(5000L);

        mailserver = new GreenMail(newSetup);
        mailserver.start();
    }

    @AfterAll
    public static void afterAll()
    {
        mailserver.stop();
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.mockConfiguration.getHost()).thenReturn(mailserver.getSmtp().getBindTo());
        when(this.mockConfiguration.getPort()).thenReturn(mailserver.getSmtp().getPort());
        when(this.mockConfiguration.getAdditionalProperties()).thenReturn(new Properties());

        MailSenderPlugin plugin = new MailSenderPlugin("dummy", "dummy", this.oldcore.getXWikiContext());
        this.api = new MailSenderPluginApi(plugin, this.oldcore.getXWikiContext());
    }

    @AfterEach
    public void afterEach() throws FolderException
    {
        mailserver.purgeEmailFromAllMailboxes();
    }

    @Test
    public void testSendMail() throws Exception
    {
        Mail mail = this.api.createMail();
        mail.setFrom("john@acme.org");
        mail.setTo("peter@acme.org");
        mail.setSubject("Test subject");
        mail.setTextPart("Text content");
        mail.setHeader("header", "value");

        assertEquals(0, this.api.sendMail(mail));

        // Verify that the email was received
        MimeMessage[] receivedEmails = mailserver.getReceivedMessages();
        assertEquals(1, receivedEmails.length);
        MimeMessage message = receivedEmails[0];
        assertEquals("Test subject", message.getSubject());
        assertEquals("john@acme.org", ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals("value", message.getHeader("header")[0]);
    }

    @Test
    public void testSendMailWithCustomConfiguration() throws Exception
    {
        Mail mail = this.api.createMail();
        mail.setFrom("john@acme.org");
        mail.setTo("peter@acme.org");
        mail.setSubject("Test subject");
        mail.setTextPart("Text content");

        MailConfiguration config = this.api.createMailConfiguration(
            new com.xpn.xwiki.api.XWiki(this.oldcore.getSpyXWiki(), this.oldcore.getXWikiContext()));
        assertEquals(mailserver.getSmtp().getPort(), config.getPort());
        assertEquals(mailserver.getSmtp().getBindTo(), config.getHost());
        assertNull(config.getFrom());

        // Modify the SMTP From value
        config.setFrom("jason@acme.org");

        assertEquals(0, this.api.sendMail(mail, config));

        // TODO: Find a way to ensure that the SMTP From value has been used.
    }

    @Test
    public void testSendRawMessage() throws MessagingException, IOException
    {
        assertEquals(0, this.api.sendRawMessage("john@acme.org", "peter@acme.org",
            "Subject:Test subject\nFrom:steve@acme.org\nCc:adam@acme.org\nheader:value\n\nTest content"));
        MimeMessage[] receivedEmails = mailserver.getReceivedMessages();
        assertEquals(2, receivedEmails.length);
        MimeMessage johnMessage = receivedEmails[0];
        assertEquals("Test subject", johnMessage.getSubject());
        assertEquals("steve@acme.org", johnMessage.getFrom()[0].toString());
        assertEquals("Test content", johnMessage.getContent());
        assertEquals("value", johnMessage.getHeader("header")[0]);
        MimeMessage peterMessage = receivedEmails[0];
        assertEquals("Test subject", peterMessage.getSubject());
        assertEquals("steve@acme.org", peterMessage.getFrom()[0].toString());
        assertEquals("Test content", peterMessage.getContent());
        assertEquals("value", peterMessage.getHeader("header")[0]);
    }
}
