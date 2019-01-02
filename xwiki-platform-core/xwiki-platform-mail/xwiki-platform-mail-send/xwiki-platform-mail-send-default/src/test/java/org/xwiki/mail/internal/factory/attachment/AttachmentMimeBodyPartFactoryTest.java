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
package org.xwiki.mail.internal.factory.attachment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.api.Attachment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.mail.internal.factory.attachment.AttachmentMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
public class AttachmentMimeBodyPartFactoryTest
{
    private static final String TEMPORARY_DIRECTORY = "target/"
        + AttachmentMimeBodyPartFactoryTest.class.getSimpleName();

    @InjectMockComponents
    private AttachmentMimeBodyPartFactory attachmentMimeBodyPartFactory;

    @InjectComponentManager
    private ComponentManager componentManager;

    @Test
    public void createAttachmentBodyPart() throws Exception
    {
        Environment environment = this.componentManager.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        Attachment attachment = mock(Attachment.class);
        when(attachment.getContentInputStream()).thenReturn(new ByteArrayInputStream("Lorem Ipsum".getBytes()));
        when(attachment.getFilename()).thenReturn("image.png");
        when(attachment.getMimeType()).thenReturn("image/png");

        MimeBodyPart part = this.attachmentMimeBodyPartFactory.create(attachment, Collections.emptyMap());

        assertEquals("<image.png>", part.getContentID());
        // JavaMail adds some extra params to the content-type header
        // (e.g. image/png; name=image.png) , we just verify the content type that we passed.
        assertTrue(part.getContentType().startsWith("image/png"));
        // We verify that the Content-Disposition has the correct file name
        assertTrue(part.getFileName().matches("image\\.png"));

        assertEquals("Lorem Ipsum", IOUtils.toString(part.getDataHandler().getInputStream(), "UTF-8"));
    }

    @Test
    public void createAttachmentBodyPartWithHeader() throws Exception
    {
        Environment environment = this.componentManager.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        Attachment attachment = mock(Attachment.class);
        when(attachment.getContentInputStream()).thenReturn(new ByteArrayInputStream("Lorem Ipsum".getBytes()));
        when(attachment.getFilename()).thenReturn("image.png");
        when(attachment.getMimeType()).thenReturn("image/png");

        Map<String, Object> parameters = Collections.singletonMap("headers", (Object)
            Collections.singletonMap("Content-Transfer-Encoding", "quoted-printable"));

        MimeBodyPart part = this.attachmentMimeBodyPartFactory.create(attachment, parameters);

        assertEquals("<image.png>", part.getContentID());
        // JavaMail adds some extra params to the content-type header
        // (e.g. image/png; name=image.png) , we just verify the content type that we passed.
        assertTrue(part.getContentType().startsWith("image/png"));
        // We verify that the Content-Disposition has the correct file name
        assertTrue(part.getFileName().matches("image\\.png"));

        assertArrayEquals(new String[]{ "quoted-printable" }, part.getHeader("Content-Transfer-Encoding"));

        assertEquals("Lorem Ipsum", IOUtils.toString(part.getDataHandler().getInputStream(), "UTF-8"));
    }

    @Test
    public void createAttachmentBodyPartWhenWriteError() throws Exception
    {
        Environment environment = this.componentManager.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File(TEMPORARY_DIRECTORY));

        Attachment attachment = mock(Attachment.class);
        when(attachment.getFilename()).thenReturn("image.png");
        when(attachment.getContent()).thenThrow(new RuntimeException("error"));

        Throwable exception = assertThrows(MessagingException.class, () -> {
            this.attachmentMimeBodyPartFactory.create(attachment, Collections.emptyMap());
        });
        assertEquals("Failed to save attachment [image.png] to the file system", exception.getMessage());
    }
}