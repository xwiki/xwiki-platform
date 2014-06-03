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
package org.xwiki.mail;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.ComponentManagerRule;

import com.icegreen.greenmail.util.GreenMail;

import static org.junit.Assert.*;

/**
 * Integration tests to prove that mail sending is working fully end to end.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class IntegrationTest
{
    @Rule
    public ComponentManagerRule componentManager = new ComponentManagerRule();

    private GreenMail mail;

    @Before
    public void startMail()
    {
        this.mail = new GreenMail();
        this.mail.start();
    }

    @After
    public void stopMail()
    {
        if (this.mail != null) {
            this.mail.stop();
        }
    }

    @Test
    @Ignore("Goal is to make this test pass!")
    public void sendMail() throws Exception
    {
        // Step 1: Create a JavaMail Session
        MailSenderConfiguration configuration = this.componentManager.getInstance(MailSenderConfiguration.class);
        Session session = Session.getInstance(configuration.getAllProperties(), new XWikiAuthenticator(configuration));

        // Step 2: Create the Message to send
        MimeMessageFactory mimeMessageFactory = this.componentManager.getInstance(MimeMessageFactory.class);
        MimeMessage message = mimeMessageFactory.create("john@doe.com", "subject", session);

        // Step 3: Add the Message Body
        Multipart multipart = new MimeMultipart("mixed");
        // Add HTML in the body
        MimeBodyPartFactory<String> htmlPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class), "html");
        multipart.addBodyPart(htmlPartFactory.create("some html here"));
        message.setContent(multipart);

        // Step 4: Send the mail
        MailSender sender = this.componentManager.getInstance(MailSender.class);
        sender.send(message, session);

        // Verify that the mail has been sent
        this.mail.waitForIncomingEmail(10000, 1);
        MimeMessage[] messages = this.mail.getReceivedMessages();
        assertEquals(1, messages.length);
    }
}
