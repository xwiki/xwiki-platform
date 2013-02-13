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
package org.xwiki.mailSender;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.mailSender.MailSender;
import org.xwiki.mailSender.internal.DefaultMailSender;
import org.xwiki.mailSender.internal.Mail;
import org.xwiki.mailSender.internal.MailConfiguration;
import org.xwiki.mailSender.internal.VCalendar;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.*;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
//import org.xwiki.test.AbstractMockingComponentTestCase;
//import org.xwiki.test.annotation.MockingRequirement;

import org.jmock.Mock;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jvnet.mock_javamail.Mailbox;

import static org.mockito.Mockito.*;

/**
 * Tests for the {@link MailSender} component.
 */
@ComponentList({DefaultMailSender.class})
public class MailSenderTest 
{
    
    @Rule
    public final MockitoComponentMockingRule<MailSender> mocker = new MockitoComponentMockingRule<MailSender>(
        DefaultMailSender.class);

    @Before
    public void configure() throws Exception
    {
        Mailbox.clearAll();
    }

    @Test
    public void testNewMail() throws Exception
    {
        Mail mail = this.mocker.getMockedComponent().newMail("john@acme.org", "peter@acme.org", "mary@acme.org", null, "Test");
        mail.addContent("text/plain", "Test");
        Assert
            .assertEquals(
                "From [john@acme.org], To [peter@acme.org], Cc [mary@acme.org], Subject [Test], Contents [text/plain:Test \n ]",
                mail.toString());
    }

    @Test
    public void MailConfiguration() throws Exception
    {
        final int port = 25;
        final DocumentAccessBridge documentAccessBridge = mock(DocumentAccessBridge.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "documentAccessBridge", documentAccessBridge);
        Properties expected = new Properties();
        expected.put("mail.smtp.port", port);
        expected.put("mail.smtp.host", "myserver");
        expected.put("mail.smtp.localhost", "localhost");
        expected.put("mail.host", "localhost");
        expected.put("mail.debug", "false");
        expected.put("mail.smtp.auth", "false");
        
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server")).thenReturn("myserver");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_port")).thenReturn(port);
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server_username")).thenReturn("");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server_password")).thenReturn("");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "javamail_extra_props")).thenReturn("");
        
        MailConfiguration conf = new MailConfiguration(documentAccessBridge) ;
        Assert.assertEquals(expected, conf.getProperties());
    }
    
    /*
    @Test
    public void testAddress() throws Exception
    {
        MailSenderUtils utils = new MailSenderUtils();
        String plain = utils.createPlain("<p>Test</p>") ;
        Assert.assertEquals("Test", plain);
        //InternetAddress[] address = utils.toInternetAddresses("john@acme.org");
        //Assert.assertEquals(1, address.length);
    }
    
    @Test
    public void testMime() throws Exception
    {
        Mail mail = this.mocker.getMockedComponent().newMail("john@acme.org", "peter@acme.org", "mary@acme.org", null, "Test");
        mail.addContent("text/html", "Test");
        MailConfiguration configuration = new MailConfiguration();
        Properties props = configuration.getProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = this.mocker.getMockedComponent().createMimeMessage(session, mail);
        Address[] recipients = message.getAllRecipients();
        Assert.assertEquals("text/html", message.getContentType());
        //Assert.assertNull(recipients);
        Assert.assertEquals("peter@acme.org", recipients[0]) ;
    }
    */
    
    @Test
    public void testSendMail() throws Exception
    {
        final int port = 25;
        final InternetAddress[] to = new InternetAddress[1];
        to[0] = new InternetAddress("john@acme.org");
        final InternetAddress[] cc = new InternetAddress[2];
        cc[0] = new InternetAddress("peter@acme.org");
        cc[1] = new InternetAddress("alfred@acme.org");

        final Logger logger = mock(Logger.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "logger", logger);
        final DocumentAccessBridge documentAccessBridge = mock(DocumentAccessBridge.class, "mockDAB");
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "documentAccessBridge", documentAccessBridge);
        final MailConfiguration mailConf = mock(MailConfiguration.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "mailConf", mailConf);
        final MailSenderUtils mailSenderUtils = mock(MailSenderUtils.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "utils", mailSenderUtils);

        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server")).thenReturn("myserver");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_port")).thenReturn(port);
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server_username")).thenReturn("");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "smtp_server_password")).thenReturn("");
        when(documentAccessBridge.getProperty("XWiki.XWikiPreferences", "javamail_extra_props")).thenReturn("");
        
        when(mailSenderUtils.toInternetAddresses("john@acme.org")).thenReturn(to);
        when(mailSenderUtils.toInternetAddresses("peter@acme.org, alfred@acme.org")).thenReturn(cc);
        //when(mailSenderUtils.createPlain("<p>Test</p>")).thenReturn("Test");
        
        Mail mail =
            this.mocker.getMockedComponent().newMail("john@acme.org", "peter@acme.org, alfred@acme.org", null, null, "Test subject");
        mail.addContent("text/html", "<p>Test</p>");
        mail.addContent("text/plain", "Test");
        int result = this.mocker.getMockedComponent().send(mail);
        verify(logger).info("Sending mail : Initializing properties");
        verify(logger).info("Message sent");
        Assert.assertEquals(1, result);

        // Verify that the email was received
        List<Message> inbox = Mailbox.get("peter@acme.org");
        Assert.assertEquals(1, inbox.size());
        Message message = inbox.get(0);
        Assert.assertEquals("Test subject", message.getSubject());
        Assert.assertEquals("john@acme.org", ((InternetAddress) message.getFrom()[0]).getAddress());
        Address[] recipients = message.getAllRecipients();
        Assert.assertEquals(2, recipients.length);
        Assert.assertEquals("alfred@acme.org", recipients[1].toString());
    }
    
    @Test
    public void testNoRecipient() throws Exception
    {
        final Logger logger = mock(Logger.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "logger", logger);
        Mail mail = this.mocker.getMockedComponent().newMail("john@acme.org", null, null, null, "Test");
        mail.addContent("text/plain", "Test");
        int result = this.mocker.getMockedComponent().send(mail);
        verify(logger).error("This mail is not valid. It should at least have one recipient and a content.");
        Assert.assertEquals(0, result);
    }

    @Test
    public void testNoContent() throws Exception
    {
        final Logger logger = mock(Logger.class);
        ReflectionUtils.setFieldValue(this.mocker.getMockedComponent(), "logger", logger);

        Mail mail = this.mocker.getMockedComponent().newMail("john@acme.org", "peter@acme.org", null, null, "Test");
        int result = this.mocker.getMockedComponent().send(mail);
        verify(logger).error("This mail is not valid. It should at least have one recipient and a content.");
        Assert.assertEquals(0, result);
    }

    @Test
    public void testCalendar() // Check that the dates in the calendar are correctly formated
    {
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 0, 1, 9, 5, 5);
        Date date = new Date();
        date.setTime(cal.getTimeInMillis());

        String calendar = (new VCalendar(date, date, "Paris", "Party")).toString();
        Scanner dateScan = new Scanner(calendar);
        dateScan.useDelimiter("DTSTART;TZID=Europe/Paris:");
        dateScan.next();
        String endCalendar = dateScan.next();
        Scanner getLine = new Scanner(endCalendar);
        getLine.useDelimiter("\n");
        Assert.assertEquals("20130101T090505Z", getLine.next());
    }
}
