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

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.environment.internal.EnvironmentConfiguration;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.factory.attachment.AttachmentMimeBodyPartFactory;
import org.xwiki.mail.internal.FileSystemMailContentStore;
import org.xwiki.mail.internal.thread.PrepareMailQueueManager;
import org.xwiki.mail.internal.DefaultMailSender;
import org.xwiki.mail.internal.thread.PrepareMailRunnable;
import org.xwiki.mail.internal.thread.SendMailQueueManager;
import org.xwiki.mail.internal.thread.SendMailRunnable;
import org.xwiki.mail.internal.factory.text.TextMimeBodyPartFactory;
import org.xwiki.mail.internal.factory.html.HTMLMimeBodyPartFactory;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Integration tests to prove that mail sending is working fully end to end with the Java API.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentList({
    TextMimeBodyPartFactory.class,
    HTMLMimeBodyPartFactory.class,
    AttachmentMimeBodyPartFactory.class,
    StandardEnvironment.class,
    DefaultMailSender.class,
    MemoryMailListener.class,
    SendMailRunnable.class,
    PrepareMailRunnable.class,
    PrepareMailQueueManager.class,
    SendMailQueueManager.class,
    FileSystemMailContentStore.class
})
public class JavaIntegrationTest
{
    @Rule
    public GreenMailRule mail = new GreenMailRule(ServerSetupTest.SMTP);

    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private TestMailSenderConfiguration configuration;

    private MimeBodyPartFactory<String> defaultBodyPartFactory;

    private MimeBodyPartFactory<String> htmlBodyPartFactory;

    private MailSender sender;

    @BeforeComponent
    public void registerConfiguration() throws Exception
    {
        this.configuration = new TestMailSenderConfiguration(
            this.mail.getSmtp().getPort(), null, null, new Properties());
        this.componentManager.registerComponent(MailSenderConfiguration.class, this.configuration);

        // Set the current wiki in the Context
        ModelContext modelContext = this.componentManager.registerMockComponent(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("wiki"));

        Provider<XWikiContext> xwikiContextProvider = this.componentManager.registerMockComponent(
            XWikiContext.TYPE_PROVIDER);
        when(xwikiContextProvider.get()).thenReturn(Mockito.mock(XWikiContext.class));

        this.componentManager.registerMockComponent(ExecutionContextManager.class);
        this.componentManager.registerMockComponent(Execution.class);

        EnvironmentConfiguration environmentConfiguration =
            this.componentManager.registerMockComponent(EnvironmentConfiguration.class);
        when(environmentConfiguration.getPermanentDirectoryPath()).thenReturn(System.getProperty("java.io.tmpdir"));
    }

    @Before
    public void initialize() throws Exception
    {
        this.defaultBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
        this.htmlBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class), "text/html");
        this.sender = this.componentManager.getInstance(MailSender.class);
    }

    @After
    public void cleanUp() throws Exception
    {
        // Make sure we stop the Mail Sender thread after each test (since it's started automatically when looking
        // up the MailSender component.
        ((DefaultMailSender) this.sender).stopMailThreads();
    }

    @Test
    public void sendTextMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        Session session = Session.getInstance(this.configuration.getAllProperties());

        // Step 2: Create the Message to send
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("john@doe.com"));

        // Step 3: Add the Message Body
        Multipart multipart = new MimeMultipart("mixed");
        // Add text in the body
        multipart.addBodyPart(this.defaultBodyPartFactory.create("some text here",
            Collections.<String, Object>singletonMap("mimetype", "text/plain")));
        message.setContent(multipart);

        // We also test using some default BCC addresses from configuration in this test
        this.configuration.setBCCAddresses(Arrays.asList("bcc1@doe.com", "bcc2@doe.com"));

        // Step 4: Send the mail and wait for it to be sent
        // Send 3 mails (3 times the same mail) to verify we can send several emails at once.
        MailListener memoryMailListener = this.componentManager.getInstance(MailListener.class, "memory");
        this.sender.sendAsynchronously(Arrays.asList(message, message, message), session, memoryMailListener);

        // Note: we don't test status reporting from the listener since this is already tested in the
        // ScriptingIntegrationTest test class.

        // Verify that the mails have been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 3);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        // Note: we're receiving 9 messages since we sent 3 with 3 recipients (2 BCC and 1 to)!
        assertEquals(9, messages.length);

        // Assert the email parts that are the same for all mails
        assertEquals("subject", messages[0].getHeader("Subject", null));
        assertEquals(1, ((MimeMultipart) messages[0].getContent()).getCount());
        BodyPart textBodyPart = ((MimeMultipart) messages[0].getContent()).getBodyPart(0);
        assertEquals("text/plain", textBodyPart.getHeader("Content-Type")[0]);
        assertEquals("some text here", textBodyPart.getContent());
        assertEquals("john@doe.com", messages[0].getHeader("To", null));

        // Note: We cannot assert that the BCC worked since by definition BCC information are not visible in received
        // messages ;) But we chekced that we received 9 emails above so that's good enough.
    }

    @Test
    public void sendHTMLAndCalendarInvitationMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        Session session = Session.getInstance(this.configuration.getAllProperties());

        // Step 2: Create the Message to send
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("john@doe.com"));

        // Step 3: Add the Message Body
        Multipart multipart = new MimeMultipart("alternative");
        // Add an HTML body part
        multipart.addBodyPart(this.htmlBodyPartFactory.create(
            "<font size=\"\\\"2\\\"\">simple meeting invitation</font>", Collections.<String, Object>emptyMap()));
        // Add the Calendar invitation body part
        String calendarContent = "BEGIN:VCALENDAR\r\n"
            + "METHOD:REQUEST\r\n"
            + "PRODID: Meeting\r\n"
            + "VERSION:2.0\r\n"
            + "BEGIN:VEVENT\r\n"
            + "DTSTAMP:20140616T164100\r\n"
            + "DTSTART:20140616T164100\r\n"
            + "DTEND:20140616T194100\r\n"
            + "SUMMARY:test request\r\n"
            + "UID:324\r\n"
            + "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:MAILTO:john@doe.com\r\n"
            + "ORGANIZER:MAILTO:john@doe.com\r\n"
            + "LOCATION:on the net\r\n"
            + "DESCRIPTION:learn some stuff\r\n"
            + "SEQUENCE:0\r\n"
            + "PRIORITY:5\r\n"
            + "CLASS:PUBLIC\r\n"
            + "STATUS:CONFIRMED\r\n"
            + "TRANSP:OPAQUE\r\n"
            + "BEGIN:VALARM\r\n"
            + "ACTION:DISPLAY\r\n"
            + "DESCRIPTION:REMINDER\r\n"
            + "TRIGGER;RELATED=START:-PT00H15M00S\r\n"
            + "END:VALARM\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mimetype", "text/calendar;method=CANCEL");
        parameters.put("headers", Collections.singletonMap("Content-Class", "urn:content-classes:calendarmessage"));
        multipart.addBodyPart(this.defaultBodyPartFactory.create(calendarContent, parameters));

        message.setContent(multipart);

        // Step 4: Send the mail and wait for it to be sent
        this.sender.sendAsynchronously(Arrays.asList(message), session, null);

        // Verify that the mail has been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 1);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        assertEquals("subject", messages[0].getHeader("Subject", null));
        assertEquals("john@doe.com", messages[0].getHeader("To", null));

        assertEquals(2, ((MimeMultipart) messages[0].getContent()).getCount());

        BodyPart htmlBodyPart = ((MimeMultipart) messages[0].getContent()).getBodyPart(0);
        assertEquals("text/html", htmlBodyPart.getHeader("Content-Type")[0]);
        assertEquals("<font size=\"\\\"2\\\"\">simple meeting invitation</font>", htmlBodyPart.getContent());

        BodyPart calendarBodyPart = ((MimeMultipart) messages[0].getContent()).getBodyPart(1);
        assertEquals("text/calendar;method=CANCEL", calendarBodyPart.getHeader("Content-Type")[0]);
        InputStream is = (InputStream) calendarBodyPart.getContent();
        assertEquals(calendarContent, IOUtils.toString(is));
    }
}
