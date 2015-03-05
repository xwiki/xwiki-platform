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
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.jmock.Expectations;
import org.jmock.Mock;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jvnet.mock_javamail.Mailbox;
import org.xwiki.mail.MailSenderConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Integration tests for {@link com.xpn.xwiki.plugin.mailsender.Mail}. The tests start a SMTP server.
 */
public class MailSenderApiTest extends AbstractBridgedXWikiComponentTestCase
{
    private Mock mockXWiki;

    private XWiki xwiki;

    private MailSenderPluginApi api;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class);
        this.xwiki = (XWiki) this.mockXWiki.proxy();
        getContext().setWiki(this.xwiki);

        // The plugin init creates a XWiki.Mail document if it doesn't exist and ensure it has the correct
        // class properties.
        this.mockXWiki.stubs().method("getDocument").with(eq("XWiki.Mail"), ANYTHING).will(
            returnValue(new XWikiDocument()));
        this.mockXWiki.stubs().method("saveDocument");

        // Register a mock Mail Sender Configuration component since it's used by MailConfiguration
        Mockery mockery = new JUnit4Mockery();
        final MailSenderConfiguration mockConfiguration =
            getComponentManager().registerMockComponent(mockery, MailSenderConfiguration.class);
        mockery.checking(new Expectations()
        {
            {
                allowing(mockConfiguration).getHost();
                will(returnValue("myserver"));
                allowing(mockConfiguration).getPort();
                will(returnValue(25));
                allowing(mockConfiguration).getFromAddress();
                will(returnValue(null));
                allowing(mockConfiguration).getUsername();
                will(returnValue(null));
                allowing(mockConfiguration).getPassword();
                will(returnValue(null));
                allowing(mockConfiguration).getAdditionalProperties();
                will(returnValue(new Properties()));
            }
        });

        MailSenderPlugin plugin = new MailSenderPlugin("dummy", "dummy", getContext());
        this.api = new MailSenderPluginApi(plugin, getContext());

        // Ensure that there are no messages in inbox
        Mailbox.clearAll();
    }

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
        List<Message> inbox = Mailbox.get("peter@acme.org");
        assertEquals(1, inbox.size());
        Message message = inbox.get(0);
        assertEquals("Test subject", message.getSubject());
        assertEquals("john@acme.org", ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals("value", message.getHeader("header")[0]);
    }

    public void testSendMailWithCustomConfiguration() throws Exception
    {
        Mail mail = this.api.createMail();
        mail.setFrom("john@acme.org");
        mail.setTo("peter@acme.org");
        mail.setSubject("Test subject");
        mail.setTextPart("Text content");

        MailConfiguration config =
            this.api.createMailConfiguration(new com.xpn.xwiki.api.XWiki(this.xwiki, getContext()));
        assertEquals(25, config.getPort());
        assertEquals("myserver", config.getHost());
        assertNull(config.getFrom());

        // Modify the SMTP From value
        config.setFrom("jason@acme.org");

        assertEquals(0, this.api.sendMail(mail, config));

        // TODO: Find a way to ensure that the SMTP From value has been used.
    }

    public void testSendRawMessage() throws MessagingException, IOException
    {
        assertEquals(0, this.api.sendRawMessage("john@acme.org", "peter@acme.org",
            "Subject:Test subject\nFrom:steve@acme.org\nCc:adam@acme.org\nheader:value\n\nTest content"));
        List<Message> inbox = Mailbox.get("peter@acme.org");
        assertEquals(1, inbox.size());
        Message message = inbox.get(0);
        assertEquals("Test subject", message.getSubject());
        assertEquals("steve@acme.org", message.getFrom()[0].toString());
        assertEquals("Test content\r\n", message.getContent());
        assertEquals("value", message.getHeader("header")[0]);

        inbox = Mailbox.get("adam@acme.org");
        assertEquals(1, inbox.size());
    }
}
