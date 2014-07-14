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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MailResultListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.XWikiAuthenticator;
import org.xwiki.mail.internal.DefaultMailSender;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Integration tests to prove that mail sending is working fully end to end with the Java API.
 *
 * @version $Id$
 * @since 6.1M2
 */
@AllComponents
public class JavaIntegrationTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private GreenMail mail;

    private MailSenderConfiguration configuration;

    private MimeBodyPartFactory<String> defaultBodyPartFactory;

    private MimeBodyPartFactory<String> htmlBodyPartFactory;

    private MailSender sender;

    private MailResultListener listener = new MailResultListener()
    {
        @Override
        public void onSuccess(MimeMessage message)
        {
            // Do nothing, we check below that the mail has been received!
        }

        @Override
        public void onError(MimeMessage message, Exception e)
        {
            // Shouldn't happen, fail the test!
            fail("Error sending mail: " + ExceptionUtils.getFullStackTrace(e));
        }
    };

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
        this.componentManager.registerMockComponent(ContextualAuthorizationManager.class);

        this.configuration = this.componentManager.getInstance(MailSenderConfiguration.class);
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
        ((DefaultMailSender) this.sender).stopMailSenderThread();
    }

    @Test
    public void sendTextMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        Session session =
            Session.getInstance(this.configuration.getAllProperties(), new XWikiAuthenticator(this.configuration));

        // Step 2: Create the Message to send
        MimeMessage message = new MimeMessage(session);
        message.setSubject("subject");
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress("john@doe.com"));

        // Step 3: Add the Message Body
        Multipart multipart = new MimeMultipart("mixed");
        // Add HTML in the body
        multipart.addBodyPart(this.defaultBodyPartFactory.create("some text here",
            Collections.<String, Object>singletonMap("mimetype", "text/plain")));
        message.setContent(multipart);

        // Step 4: Send the mail and wait for it to be sent
        // Send 3 mails (3 times the same mail) to verify we can send several emails at once.
        this.sender.sendAsynchronously(message, session, this.listener);
        this.sender.sendAsynchronously(message, session, this.listener);
        this.sender.sendAsynchronously(message, session, this.listener);
        this.sender.waitTillSent(10000L);

        // Verify that the mails have been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 3);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        assertEquals("subject", messages[0].getHeader("Subject")[0]);
        assertEquals("john@doe.com", messages[0].getHeader("To")[0]);

        assertEquals(1, ((MimeMultipart) messages[0].getContent()).getCount());

        BodyPart textBodyPart = ((MimeMultipart) messages[0].getContent()).getBodyPart(0);
        assertEquals("text/plain", textBodyPart.getHeader("Content-Type")[0]);
        assertEquals("some text here", textBodyPart.getContent());
    }

    @Test
    public void sendHTMLAndCalendarInvitationMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        Session session =
            Session.getInstance(this.configuration.getAllProperties(), new XWikiAuthenticator(this.configuration));

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
        this.sender.send(message, session);

        // Verify that the mail has been received (wait maximum 10 seconds).
        this.mail.waitForIncomingEmail(10000L, 1);
        MimeMessage[] messages = this.mail.getReceivedMessages();

        assertEquals("subject", messages[0].getHeader("Subject")[0]);
        assertEquals("john@doe.com", messages[0].getHeader("To")[0]);

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
