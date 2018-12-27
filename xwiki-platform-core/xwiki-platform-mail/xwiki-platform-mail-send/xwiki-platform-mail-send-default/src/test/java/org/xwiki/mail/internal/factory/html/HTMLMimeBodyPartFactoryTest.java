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
package org.xwiki.mail.internal.factory.html;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.api.Attachment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.html.HTMLMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
public class HTMLMimeBodyPartFactoryTest
{
    @InjectMockComponents
    private HTMLMimeBodyPartFactory htmlMimeBodyPartFactory;

    @InjectComponentManager
    private ComponentManager componentManager;

    @Test
    public void createWhenOnlyHTMLContent() throws Exception
    {
        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create("<p>some html</p>", Collections.emptyMap());

        assertEquals("<p>some html</p>", bodyPart.getContent());
        assertEquals("text/html; charset=UTF-8", bodyPart.getContentType());
    }

    @Test
    public void createWhenHTMLContentAndHeader() throws Exception
    {
        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create("<p>some html</p>",
            Collections.singletonMap("headers", Collections.singletonMap("key", "value")));

        assertEquals("<p>some html</p>", bodyPart.getContent());
        assertArrayEquals(new String[]{ "value" }, bodyPart.getHeader("key"));
    }

    @Test
    public void createWhenHTMLAndAlternateTextContent() throws Exception
    {
        MimeBodyPart textBodyPart = mock(MimeBodyPart.class);
        MimeBodyPartFactory defaultBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
        when(defaultBodyPartFactory.create(eq("some text"), any(Map.class))).thenReturn(textBodyPart);

        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create("<p>some html</p>",
            Collections.singletonMap("alternate", "some text"));

        MimeMultipart multipart = ((MimeMultipart) bodyPart.getContent());
        assertEquals(2, multipart.getCount());
        assertSame(textBodyPart, multipart.getBodyPart(0));
        assertEquals("<p>some html</p>", multipart.getBodyPart(1).getContent());
    }

    @Test
    public void createWhenHTMLAndEmbeddedImages() throws Exception
    {
        Attachment attachment = mock(Attachment.class);

        MimeBodyPart attachmentBodyPart = mock(MimeBodyPart.class);
        MimeBodyPartFactory attachmentBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, Attachment.class), "xwiki/attachment");
        when(attachmentBodyPartFactory.create(same(attachment), any(Map.class))).thenReturn(attachmentBodyPart);

        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create("<p>some html</p>",
            Collections.singletonMap("attachments", Arrays.asList(attachment)));

        MimeMultipart multipart = ((MimeMultipart) bodyPart.getContent());
        assertEquals(2, multipart.getCount());
        assertEquals("<p>some html</p>", multipart.getBodyPart(0).getContent());
        assertSame(attachmentBodyPart, multipart.getBodyPart(1));
    }

    @Test
    public void createWhenHTMLAndEmbeddedImagesAndNormalAttachments() throws Exception
    {
        Attachment normalAttachment = mock(Attachment.class, "normalAttachment");
        when(normalAttachment.getFilename()).thenReturn("attachment1.png");
        Attachment embeddedAttachment = mock(Attachment.class, "embeddedAttachment");
        when(embeddedAttachment.getFilename()).thenReturn("embeddedAttachment.png");

        MimeBodyPart embeddedAttachmentBodyPart = mock(MimeBodyPart.class);
        MimeBodyPartFactory attachmentBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, Attachment.class), "xwiki/attachment");
        when(attachmentBodyPartFactory.create(same(embeddedAttachment), any(Map.class))).thenReturn(
            embeddedAttachmentBodyPart);

        MimeBodyPart normalAttachmentBodyPart = mock(MimeBodyPart.class);
        when(attachmentBodyPartFactory.create(same(normalAttachment), any(Map.class))).thenReturn(
            normalAttachmentBodyPart);

        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create(
            "<p>html... <img src='cid:embeddedAttachment.png'/></p>",
            Collections.singletonMap("attachments", Arrays.asList(normalAttachment, embeddedAttachment)));

        MimeMultipart multipart = (MimeMultipart) bodyPart.getContent();
        assertEquals(2, multipart.getCount());

        MimeMultipart htmlMultipart = (MimeMultipart) multipart.getBodyPart(0).getContent();
        assertEquals(2, htmlMultipart.getCount());
        assertEquals("<p>html... <img src='cid:embeddedAttachment.png'/></p>",
            htmlMultipart.getBodyPart(0).getContent());
        assertSame(embeddedAttachmentBodyPart, htmlMultipart.getBodyPart(1));

        assertSame(normalAttachmentBodyPart, multipart.getBodyPart(1));
    }

    @Test
    public void createWhenHTMLAndAlternateTextAndEmbeddedImagesAndNormalAttachments() throws Exception
    {
        Attachment normalAttachment = mock(Attachment.class, "normalAttachment");
        when(normalAttachment.getFilename()).thenReturn("attachment1.png");
        Attachment embeddedAttachment = mock(Attachment.class, "embeddedAttachment");
        when(embeddedAttachment.getFilename()).thenReturn("embeddedAttachment.png");

        MimeBodyPart embeddedAttachmentBodyPart = mock(MimeBodyPart.class);
        MimeBodyPartFactory attachmentBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, Attachment.class), "xwiki/attachment");
        when(attachmentBodyPartFactory.create(same(embeddedAttachment), any(Map.class))).thenReturn(
            embeddedAttachmentBodyPart);

        MimeBodyPart normalAttachmentBodyPart = mock(MimeBodyPart.class);
        when(attachmentBodyPartFactory.create(same(normalAttachment), any(Map.class))).thenReturn(
            normalAttachmentBodyPart);

        MimeBodyPart textBodyPart = mock(MimeBodyPart.class);
        MimeBodyPartFactory defaultBodyPartFactory = this.componentManager.getInstance(
            new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
        when(defaultBodyPartFactory.create(eq("some text"), any(Map.class))).thenReturn(textBodyPart);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("attachments", Arrays.asList(normalAttachment, embeddedAttachment));
        parameters.put("alternate", "some text");
        MimeBodyPart bodyPart = this.htmlMimeBodyPartFactory.create(
            "<p>html... <img src='cid:embeddedAttachment.png'/></p>", parameters);

        MimeMultipart multipart = (MimeMultipart) bodyPart.getContent();
        assertEquals(2, multipart.getCount());

        MimeMultipart alternateMultipart = (MimeMultipart) multipart.getBodyPart(0).getContent();
        assertEquals(2, alternateMultipart.getCount());
        assertSame(textBodyPart, alternateMultipart.getBodyPart(0));

        MimeMultipart relatedMultipart = (MimeMultipart) alternateMultipart.getBodyPart(1).getContent();
        assertEquals(2, relatedMultipart.getCount());
        assertEquals("<p>html... <img src='cid:embeddedAttachment.png'/></p>",
            relatedMultipart.getBodyPart(0).getContent());
        assertSame(embeddedAttachmentBodyPart, relatedMultipart.getBodyPart(1));

        assertSame(normalAttachmentBodyPart, multipart.getBodyPart(1));
    }
}