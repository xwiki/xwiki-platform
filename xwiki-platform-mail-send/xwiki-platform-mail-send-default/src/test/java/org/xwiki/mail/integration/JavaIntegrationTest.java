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
import java.util.Collections;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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
import org.xwiki.test.ComponentManagerRule;
import org.xwiki.test.annotation.AllComponents;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static org.junit.Assert.*;

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
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    private GreenMail mail;

    private MailSenderConfiguration configuration;

    private MimeBodyPartFactory<String> defaultBodyPartFactory;

    private MailSender sender;

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
        this.configuration = this.componentManager.getInstance(MailSenderConfiguration.class);
        this.defaultBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
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
    public void sendMail() throws Exception
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
        MailResultListener listener = new MailResultListener()
        {
            @Override
            public void onSuccess(MimeMessage message)
            {
                // Do nothing, we check below that the mail has been received!
            }

            @Override
            public void onError(MimeMessage message, Throwable t)
            {
                // Shouldn't happen, fail the test!
                fail("Error sending mail: " + ExceptionUtils.getFullStackTrace(t));
            }
        };

        // Send 3 mails (3 times the same mail)
        this.sender.send(message, session, listener);
        this.sender.send(message, session, listener);
        this.sender.send(message, session, listener);

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
