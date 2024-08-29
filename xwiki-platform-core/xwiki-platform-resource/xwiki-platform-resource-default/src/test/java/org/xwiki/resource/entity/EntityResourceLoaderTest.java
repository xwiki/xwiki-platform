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
package org.xwiki.resource.entity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.resource.internal.entity.EntityResourceLoader;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.resource.internal.entity.EntityResourceLoader}.
 *
 * @version $Id$
 */
@ComponentTest
class EntityResourceLoaderTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @InjectMockComponents
    private EntityResourceLoader resourceLoader;

    @MockComponent
    private DocumentAccessBridge dab;

    @Test
    void loadWhenAttachmentReference() throws Exception
    {
        AttachmentReference attachmentReference = new AttachmentReference("file",
            new DocumentReference("wiki", "space", "page"));
        EntityResourceReference resourceReference = new EntityResourceReference(attachmentReference,
            EntityResourceAction.VIEW);

        when(this.dab.getAttachmentContent((EntityReference) attachmentReference)).thenReturn(
            new ByteArrayInputStream("content".getBytes()));

        InputStream is = this.resourceLoader.load(resourceReference);
        assertEquals("content", IOUtils.toString(is, "UTF-8"));
    }

    @Test
    void loadWhenPageAttachmentReference() throws Exception
    {
        PageAttachmentReference pageAttachmentReference = new PageAttachmentReference("file",
            new PageReference("wiki", "page1", "page2"));
        EntityResourceReference resourceReference = new EntityResourceReference(pageAttachmentReference,
            EntityResourceAction.VIEW);

        when(this.dab.getAttachmentContent(pageAttachmentReference)).thenReturn(
            new ByteArrayInputStream("content".getBytes()));

        InputStream is = this.resourceLoader.load(resourceReference);
        assertEquals("content", IOUtils.toString(is, "UTF-8"));
    }

    @Test
    void loadWhenException() throws Exception
    {
        AttachmentReference attachmentReference = new AttachmentReference("file",
            new DocumentReference("wiki", "space", "page"));
        EntityResourceReference resourceReference = new EntityResourceReference(attachmentReference,
            EntityResourceAction.VIEW);

        when(this.dab.getAttachmentContent((EntityReference) attachmentReference)).thenThrow(new Exception("error"));

        assertNull(this.resourceLoader.load(resourceReference));

        assertEquals("Failed to get attachment's content for [Attachment wiki:space.page@file]",
            logCapture.getMessage(0));
    }
}
