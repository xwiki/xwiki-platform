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
package org.xwiki.classloader.internal.protocol.attachmentjar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachmentURLStreamHandler}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AttachmentURLStreamHandlerTest
{
    @InjectMockComponents
    private AttachmentURLStreamHandler handler;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> arf;

    @MockComponent
    private DocumentAccessBridge dab;

    @Test
    void invalidAttachmentJarURL() throws Exception
    {
        URL url = new URL(null, "http://invalid/url", this.handler);

        try {
            url.openConnection();
            fail("Should have thrown an exception here");
        } catch (RuntimeException expected) {
            assertEquals("An attachment JAR URL should start with [attachmentjar://], got [http://invalid/url]",
                expected.getMessage());
        }
    }

    @Test
    void attachmentJarURL() throws Exception
    {
        URL url = new URL(null, "attachmentjar://Space.Page@filename", this.handler);

        final AttachmentReference attachmentReference = new AttachmentReference("filename",
            new DocumentReference("wiki", "space", "page"));

        when(this.arf.resolve("Space.Page@filename")).thenReturn(attachmentReference);
        when(this.dab.getAttachmentContent(attachmentReference))
            .thenReturn(new ByteArrayInputStream("content".getBytes()));

        URLConnection connection = url.openConnection();
        InputStream input = null;
        try {
            connection.connect();
            input = connection.getInputStream();
            assertEquals("content", IOUtils.toString(input));
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Verify that URL-encoded chars are decoded.
     */
    @Test
    void attachmentJarURLWithEncodedChars() throws Exception
    {
        URL url = new URL(null, "attachmentjar://some%20page", this.handler);
        url.openConnection();
        verify(this.arf).resolve("some page");
    }
}
