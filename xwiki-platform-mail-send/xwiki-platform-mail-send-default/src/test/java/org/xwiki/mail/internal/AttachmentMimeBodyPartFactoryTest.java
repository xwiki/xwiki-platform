/*
 *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.mail.internal;

import java.io.File;

import javax.mail.internet.MimeBodyPart;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.api.Attachment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachmentMimeBodyPartFactory}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class AttachmentMimeBodyPartFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<AttachmentMimeBodyPartFactory> mocker =
            new MockitoComponentMockingRule<>(AttachmentMimeBodyPartFactory.class);


    @Test
    public void createAttachmentBodyPart() throws Exception
    {
        Environment environment = this.mocker.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(new File("/tmpdir"));

        Attachment attachment = mock(Attachment.class);
        byte[] fileContent = "Lorem Ipsum".getBytes();
        when(attachment.getFilename()).thenReturn("image.png");
        when(attachment.getMimeType()).thenReturn("image/png;");
        when(attachment.getContent()).thenReturn(fileContent);

        MimeBodyPart attachmentPart = this.mocker.getComponentUnderTest().create(attachment);

        verify(attachment).getFilename();
        verify(attachment).getMimeType();
        verify(attachment).getContent();
    }
}
