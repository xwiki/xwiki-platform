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
package org.xwiki.mail.internal.factory.text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TextMimeMessageFactory}.
 *
 * @version $Id$
 */
@ComponentTest
class TextMimeMessageFactoryTest
{
    @InjectMockComponents
    private TextMimeMessageFactory textMimeMessageFactory;

    @MockComponent
    private ConverterManager converterManager;

    @MockComponent
    private MimeBodyPartFactory<String> mimeBodyPartFactory;

    @Test
    void createMessage() throws MessagingException, IOException
    {
        String source = "Some mail content";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("to", "toto@xwiki.com");
        parameters.put("from", "admin@xwiki.com");
        parameters.put("type", "text");
        parameters.put("subject", "important email");

        Address from = new InternetAddress("admin@xwiki.com");
        Address to = new InternetAddress("toto@xwiki.com");

        when(this.converterManager.convert(Address[].class, "toto@xwiki.com")).thenReturn(new Address[] {to});
        when(this.converterManager.convert(Address.class, "admin@xwiki.com")).thenReturn(from);

        MimeBodyPart bodyPart = mock(MimeBodyPart.class);
        when(this.mimeBodyPartFactory.create(source, parameters)).thenReturn(bodyPart);

        ExtendedMimeMessage expectedMessage = new ExtendedMimeMessage();
        expectedMessage.setFrom(from);
        expectedMessage.setRecipient(Message.RecipientType.TO, to);
        expectedMessage.setSubject("important email");
        expectedMessage.setType("text");
        Multipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(bodyPart);
        expectedMessage.setContent(multipart);

        MimeMessage message = this.textMimeMessageFactory.createMessage(source, parameters);
        assertEquals(to, message.getRecipients(Message.RecipientType.TO)[0]);
        assertEquals(from, message.getFrom()[0]);
        assertEquals("important email", message.getSubject());
        assertEquals(bodyPart, ((Multipart) message.getContent()).getBodyPart(0));
    }
}