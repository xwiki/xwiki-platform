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
package org.xwiki.mail.internal.factory.message;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.message.MessageMimeMessageFactory}.
 *
 * @version $Id$
 */
@ComponentTest
public class MessageMimeMessageFactoryTest
{
    @InjectMockComponents
    private MessageMimeMessageFactory mimeMessageFactory;

    @Test
    public void createMessageWithBadSource()
    {
        Throwable exception = assertThrows(MessagingException.class, () -> {
            this.mimeMessageFactory.createMessage("source", null);
        });
        assertEquals("Failed to create mime message from source [class java.lang.String]", exception.getMessage());
    }

    @Test
    public void createMultipleMessages() throws Exception
    {
        MimeMessage source = new MimeMessage(Session.getInstance(new Properties()));
        source.setFrom(new InternetAddress("localhost@xwiki.org"));
        source.addRecipient(Message.RecipientType.TO, new InternetAddress("john@doe.com"));
        source.setSubject("Subject");
        source.setText("Content");

        MimeMessage first =this.mimeMessageFactory.createMessage(source, null);

        assertEqualMimeMessage(first, source);

        first.setFrom(new InternetAddress("jane@doe.com"));
        first.addRecipient(Message.RecipientType.TO, new InternetAddress("jack@doe.com"));
        first.setSubject("First subject");
        first.setText("First content");

        MimeMessage second = this.mimeMessageFactory.createMessage(source, null);

        // Ensure second message is similar to source, and not to modified first
        assertEqualMimeMessage(second, source);
    }

    @Test
    public void ensureMessageReceiveSameMessageID() throws Exception
    {
        MimeMessage source = new MimeMessage(Session.getInstance(new Properties()));
        source.setText("Content");

        MimeMessage first = this.mimeMessageFactory.createMessage(source, null);
        MimeMessage second = this.mimeMessageFactory.createMessage(source, null);

        // Ensure second message is similar to source, and not to modified first
        assertThat(first.getMessageID(), notNullValue());
        assertThat(second.getMessageID(), notNullValue());
        assertThat(first.getMessageID(), equalTo(second.getMessageID()));
    }

    private void assertEqualMimeMessage(MimeMessage message1, MimeMessage message2) throws Exception
    {
        assertThat(message1.getFrom(), equalTo(message2.getFrom()));
        assertThat(message1.getAllRecipients(), equalTo(message2.getAllRecipients()));
        assertThat(message1.getSubject(), equalTo(message2.getSubject()));
        assertThat(message1.getContent(), equalTo(message2.getContent()));
    }
}
