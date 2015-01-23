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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.TemplateMimeMessageFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class TemplateMimeMessageFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<TemplateMimeMessageFactory> mocker =
        new MockitoComponentMockingRule<>(TemplateMimeMessageFactory.class);

    private Session session;

    private DocumentReference templateReference;

    private MimeBodyPart mimeBodyPart;

    @Before
    public void setUp() throws Exception
    {
        this.templateReference = new DocumentReference("templatewiki", "templatespace", "templatepage");

        MailTemplateManager mailTemplateManager = this.mocker.getInstance(MailTemplateManager.class);
        when(mailTemplateManager.evaluate(same(this.templateReference), eq("subject"),
            anyMapOf(String.class, String.class), any(Locale.class))).thenReturn("XWiki news");

        MimeBodyPartFactory<DocumentReference> templateBodyPartFactory = this.mocker.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, DocumentReference.class), "xwiki/template");
        this.mimeBodyPart = mock(MimeBodyPart.class);
        when(templateBodyPartFactory.create(same(this.templateReference),
            anyMapOf(String.class, Object.class))).thenReturn(this.mimeBodyPart);

        this.session = Session.getDefaultInstance(new Properties());
    }

    @Test
    public void createMessage() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language", "fr");
        parameters.put("velocityVariables", Collections.<String, Object>singletonMap("company", "XWiki"));

        MimeMessage message =
            this.mocker.getComponentUnderTest().createMessage(this.session, this.templateReference, parameters);

        assertEquals("XWiki news", message.getSubject());

        // Also verify that a body part has been added
        assertEquals(this.mimeBodyPart, ((MimeMultipart) message.getContent()).getBodyPart(0));
    }

    @Test
    public void createMessageWithToFromCCAndBCCAddressesAsStrings() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("language", "fr");
        parameters.put("velocityVariables", Collections.<String, Object>singletonMap("company", "XWiki"));
        parameters.put("to", "to@doe.com");
        parameters.put("cc", "cc@doe.com");
        parameters.put("bcc", "bcc@doe.com");
        parameters.put("from", "from@doe.com");

        ConverterManager converterManager = this.mocker.getInstance(ConverterManager.class);
        when(converterManager.convert(Address[].class, "to@doe.com")).thenReturn(InternetAddress.parse("to@doe.com"));
        when(converterManager.convert(Address[].class, "cc@doe.com")).thenReturn(InternetAddress.parse("cc@doe.com"));
        when(converterManager.convert(Address[].class, "bcc@doe.com")).thenReturn(InternetAddress.parse("bcc@doe.com"));
        when(converterManager.convert(Address.class, "from@doe.com")).thenReturn(
            InternetAddress.parse("from@doe.com")[0]);

        MimeMessage message =
            this.mocker.getComponentUnderTest().createMessage(this.session, this.templateReference, parameters);

        assertEquals("XWiki news", message.getSubject());
        assertArrayEquals(InternetAddress.parse("from@doe.com"), message.getFrom());
        assertArrayEquals(InternetAddress.parse("to@doe.com"), message.getRecipients(Message.RecipientType.TO));
        assertArrayEquals(InternetAddress.parse("cc@doe.com"), message.getRecipients(Message.RecipientType.CC));
        assertArrayEquals(InternetAddress.parse("bcc@doe.com"), message.getRecipients(Message.RecipientType.BCC));

        // Also verify that a body part has been added
        assertEquals(this.mimeBodyPart, ((MimeMultipart) message.getContent()).getBodyPart(0));
    }
}