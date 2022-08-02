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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.template.TemplateMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentTest
public class TemplateMimeBodyPartFactoryTest
{
    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    @InjectMockComponents
    private TemplateMimeBodyPartFactory templateMimeBodyPartFactory;

    @MockComponent
    private MailTemplateManager templateManager;

    @MockComponent
    @Named("text/html")
    private MimeBodyPartFactory<String> htmlMimeBodyPartFactory;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private AttachmentConverter attachmentConverter;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.templateManager.evaluate(this.documentReference, "text", new HashMap<>(), null))
            .thenReturn("Hello John Doe, john@doe.com");
        when(this.templateManager.evaluate(this.documentReference, "html", new HashMap<>(), null))
            .thenReturn("Hello <b>John Doe</b> <br />john@doe.com");
    }

    @Test
    void createWithoutAttachment() throws Exception
    {
        this.templateMimeBodyPartFactory.create(this.documentReference,
            Collections.singletonMap("velocityVariables", new HashMap<String, String>()));

        verify(this.htmlMimeBodyPartFactory).create("Hello <b>John Doe</b> <br />john@doe.com",
            Collections.singletonMap("alternate", "Hello John Doe, john@doe.com"));
    }

    @Test
    void createWithAttachment() throws Exception
    {
        Attachment attachment = mock(Attachment.class);
        List<Attachment> attachments = Collections.singletonList(attachment);

        Map<String, Object> bodyPartParameters = new HashMap<>();
        bodyPartParameters.put("velocityVariables", new HashMap<String, String>());
        bodyPartParameters.put("attachments", attachments);

        this.templateMimeBodyPartFactory.create(this.documentReference, bodyPartParameters);

        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternate", "Hello John Doe, john@doe.com");
        htmlParameters.put("attachments", attachments);

        verify(this.htmlMimeBodyPartFactory).create("Hello <b>John Doe</b> <br />john@doe.com", htmlParameters);
    }

    @Test
    void createWithAttachmentAndTemplateAttachments() throws Exception
    {
        Attachment attachment1 = mock(Attachment.class, "attachment1");
        Map<String, Object> bodyPartParameters = new HashMap<>();
        bodyPartParameters.put("velocityVariables", new HashMap<String, String>());
        bodyPartParameters.put("attachments", Collections.singletonList(attachment1));
        bodyPartParameters.put("includeTemplateAttachments", true);

        // Mock the retrieval and conversion of attachments from the Template document
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(this.dab.getDocumentInstance(this.documentReference)).thenReturn(xwikiDocument);
        XWikiAttachment xwikiAttachment = mock(XWikiAttachment.class);
        when(xwikiDocument.getAttachmentList()).thenReturn(Collections.singletonList(xwikiAttachment));
        Attachment attachment2 = mock(Attachment.class, "attachment2");
        when(this.attachmentConverter.convert(Collections.singletonList(xwikiAttachment))).thenReturn(
            Collections.singletonList(attachment2));

        this.templateMimeBodyPartFactory.create(this.documentReference, bodyPartParameters);

        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternate", "Hello John Doe, john@doe.com");
        htmlParameters.put("attachments", Arrays.asList(attachment1, attachment2));

        verify(this.htmlMimeBodyPartFactory).create("Hello <b>John Doe</b> <br />john@doe.com", htmlParameters);
    }

    @Test
    void createWithAttachmentAndTemplateAttachmentsWhenError() throws Exception
    {
        Attachment attachment1 = mock(Attachment.class, "attachment1");
        Map<String, Object> bodyPartParameters = new HashMap<>();
        bodyPartParameters.put("velocityVariables", new HashMap<String, String>());
        bodyPartParameters.put("attachments", Collections.singletonList(attachment1));
        bodyPartParameters.put("includeTemplateAttachments", true);

        // Mock the retrieval and conversion of attachments from the Template document
        when(this.dab.getDocumentInstance(this.documentReference)).thenThrow(new Exception("error"));

        Throwable exception = assertThrows(MessagingException.class, () -> {
            this.templateMimeBodyPartFactory.create(this.documentReference, bodyPartParameters);
        });
        assertEquals("Failed to include attachments from the Mail Template [wiki:space.page]", exception.getMessage());

        verifyNoMoreInteractions(this.htmlMimeBodyPartFactory);
    }
}