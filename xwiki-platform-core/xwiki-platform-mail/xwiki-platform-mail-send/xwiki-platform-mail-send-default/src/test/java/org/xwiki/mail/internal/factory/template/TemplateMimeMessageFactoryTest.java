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
package org.xwiki.mail.internal.factory.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.TemplateMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentTest
public class TemplateMimeMessageFactoryTest
{
    @InjectMockComponents
    private TemplateMimeMessageFactory templateMimeMessageFactory;

    @MockComponent
    private MailTemplateManager mailTemplateManager;

    @MockComponent
    @Named("xwiki/template")
    private MimeBodyPartFactory<DocumentReference> templateBodyPartFactory;

    @MockComponent
    private ConverterManager converterManager;

    private DocumentReference templateReference;

    private MimeBodyPart mimeBodyPart;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateReference = new DocumentReference("templatewiki", "templatespace", "templatepage");

        when(this.mailTemplateManager.evaluate(same(this.templateReference), eq("subject"), any(), any()))
            .thenReturn("XWiki news");

        this.mimeBodyPart = mock(MimeBodyPart.class);
        when(this.templateBodyPartFactory.create(same(this.templateReference), any())).thenReturn(this.mimeBodyPart);
    }

    @Test
    void createMessage() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language", "fr");
        parameters.put("velocityVariables", Collections.<String, Object>singletonMap("company", "XWiki"));

        MimeMessage message =this.templateMimeMessageFactory.createMessage(this.templateReference, parameters);

        assertEquals("XWiki news", message.getSubject());

        // Also verify that a body part has been added
        assertEquals(this.mimeBodyPart, ((MimeMultipart) message.getContent()).getBodyPart(0));
    }

    @Test
    void createMessageWithToFromCCAndBCCAddressesAsStrings() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language", "fr");
        parameters.put("velocityVariables", Collections.<String, Object>singletonMap("company", "XWiki"));
        parameters.put("to", "to@doe.com");
        parameters.put("cc", "cc@doe.com");
        parameters.put("bcc", "bcc@doe.com");
        parameters.put("from", "from@doe.com");

        when(this.converterManager.convert(Address[].class, "to@doe.com")).thenReturn(
            InternetAddress.parse("to@doe.com"));
        when(this.converterManager.convert(Address[].class, "cc@doe.com")).thenReturn(
            InternetAddress.parse("cc@doe.com"));
        when(this.converterManager.convert(Address[].class, "bcc@doe.com")).thenReturn(
            InternetAddress.parse("bcc@doe.com"));
        when(this.converterManager.convert(Address.class, "from@doe.com")).thenReturn(
            InternetAddress.parse("from@doe.com")[0]);

        MimeMessage message = this.templateMimeMessageFactory.createMessage(this.templateReference, parameters);

        assertEquals("XWiki news", message.getSubject());
        assertArrayEquals(InternetAddress.parse("from@doe.com"), message.getFrom());
        assertArrayEquals(InternetAddress.parse("to@doe.com"), message.getRecipients(Message.RecipientType.TO));
        assertArrayEquals(InternetAddress.parse("cc@doe.com"), message.getRecipients(Message.RecipientType.CC));
        assertArrayEquals(InternetAddress.parse("bcc@doe.com"), message.getRecipients(Message.RecipientType.BCC));

        // Also verify that a body part has been added
        assertEquals(this.mimeBodyPart, ((MimeMultipart) message.getContent()).getBodyPart(0));
    }
}
