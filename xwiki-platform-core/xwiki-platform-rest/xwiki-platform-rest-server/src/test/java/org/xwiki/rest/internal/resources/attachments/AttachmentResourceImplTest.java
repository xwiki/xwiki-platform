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
package org.xwiki.rest.internal.resources.attachments;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.xwiki.internal.attachment.XWikiAttachmentSecurityManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachmentResourceImpl}.
 *
 * @version $Id$
 */
@ComponentTest
class AttachmentResourceImplTest
{
    private static final String FILENAME = "image.png";

    @InjectMockComponents
    private AttachmentResourceImpl attachmentResource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private XWikiAttachmentSecurityManager attachmentSecurityManager;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.xcontextProvider.get()).thenReturn(mock());
    }

    @Test
    void answerWithAttachment() throws Exception
    {
        byte[] content = new byte[] { 1, 2, 3 };
        Attachment attachment = mock();
        when(attachment.getFilename()).thenReturn(FILENAME);
        when(attachment.getMimeType()).thenReturn("image/png");
        when(attachment.getContent()).thenReturn(content);
        when(this.attachmentSecurityManager.shouldBeDownloaded(attachment)).thenReturn(false);
        when(this.attachmentSecurityManager.getContentDispositionHeader(FILENAME, false))
            .thenReturn("inline; filename*=utf-8''image.png");

        Response response = this.attachmentResource.answerWithAttachment(attachment);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("image/png", response.getMediaType().toString());
        assertArrayEquals(content, (byte[]) response.getEntity());
        assertEquals("inline; filename*=utf-8''image.png", response.getHeaderString("Content-Disposition"));
        assertEquals("nosniff", response.getHeaderString("X-Content-Type-Options"));
    }
}
