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
import java.util.Collections;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.internal.DefaultMailSender;
import org.xwiki.mail.script.MailSenderScriptService;
import org.xwiki.mail.script.MimeMessageWrapper;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.AllComponents;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.assertEquals;

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
    public void sendTextMail() throws Exception
    {
        // Set the EC
        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());

        MimeMessageWrapper message = this.scriptService.createMessage("john@doe.com", "subject");
        message.addPart("text/plain", "some text here");

        // Send 3 mails (3 times the same mail) to verify we can send several emails at once.
        message.send();
        message.send();
        message.send();
        message.waitTillSent(10000L);

        // Verify that there are no errors
        assertEquals(0, message.getErrors().size());

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
        // Set the EC
        Execution execution = this.componentManager.getInstance(Execution.class);
        execution.setContext(new ExecutionContext());

        MimeMessageWrapper message = this.scriptService.createMessage("john@doe.com", "subject");
        message.addPart("text/html", "<font size=\"\\\"2\\\"\">simple meeting invitation</font>");
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
        message.addPart("text/calendar;method=CANCEL", calendarContent,
            Collections.<String, Object>singletonMap("headers",
                Collections.singletonMap("Content-Class", "urn:content-classes:calendarmessage")));

        message.send();
        message.waitTillSent(10000L);

        // Verify that there are no errors
        assertEquals(0, message.getErrors().size());

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
