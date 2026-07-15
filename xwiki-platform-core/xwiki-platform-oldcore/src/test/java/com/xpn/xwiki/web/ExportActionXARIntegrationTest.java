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
package com.xpn.xwiki.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.xwiki.filter.instance.internal.InstanceModel;
import org.xwiki.filter.instance.internal.input.InstanceInputFilterStream;
import org.xwiki.filter.instance.internal.input.InstanceInputFilterStreamFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.internal.filter.input.DocumentInstanceInputEventGenerator;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link ExportAction} that verifies XAR exports produce valid ZIP archives even when the content
 * exceeds the 4 GB Zip64 threshold.
 *
 * <p>This test creates a document with a large attachment whose base64-encoded representation in the XAR exceeds 4 GB,
 * triggering the need for Zip64 format in the ZIP archive. The attachment content is a repeating byte pattern that
 * compresses extremely well, so the actual disk usage remains minimal (a few KB) despite the large logical size.</p>
 */
@OldcoreTest
@XWikiDocumentFilterUtilsComponentList
@ComponentList({
    InstanceInputFilterStreamFactory.class,
    InstanceInputFilterStream.class,
    DocumentInstanceInputEventGenerator.class
})
class ExportActionXARIntegrationTest
{
    /**
     * Attachment size slightly over 3 GB. After base64 encoding (4/3 expansion ratio), the resulting XML content in the
     * ZIP entry exceeds 4 GB (0xFFFFFFFF), which is the threshold requiring Zip64 format.
     */
    private static final long ATTACHMENT_SIZE = 3L * 1024 * 1024 * 1024 + 100L * 1024 * 1024;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private DocumentSelectionResolver documentSelectionResolver;

    @MockComponent
    private InstanceModel instanceModel;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiResponse response;

    @XWikiTempDir
    private File tempDir;

    private StubServletOutputStream servletOutputStream;

    @InjectMockComponents
    private ExportAction action;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void configure() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setRequest(this.request);

        this.servletOutputStream = new StubServletOutputStream();
        when(this.response.getOutputStream()).thenReturn(this.servletOutputStream);
        context.setResponse(this.response);

        // XAR export requires admin rights.
        when(this.oldcore.getMockRightService().hasWikiAdminRights(context)).thenReturn(true);

        // Request parameters for XAR export.
        when(this.request.get("format")).thenReturn("xar");
        when(this.request.get("name")).thenReturn("large-export");

        // Set up a document with a large attachment whose content is generated on the fly.
        DocumentReference docRef = new DocumentReference("xwiki", "Space", "LargeDoc");
        XWikiDocument document = new XWikiDocument(docRef);
        document.setDate(new Date(0));
        document.setCreationDate(new Date(0));
        document.setContentUpdateDate(new Date(0));

        XWikiAttachment attachment = new XWikiAttachment()
        {
            @Override
            public InputStream getContentInputStream(XWikiContext xcontext)
            {
                return new RepetitiveInputStream(ATTACHMENT_SIZE, (byte) 42);
            }

            @Override
            public long getLongSize()
            {
                return ATTACHMENT_SIZE;
            }
        };
        attachment.setFilename("large.bin");
        attachment.setDate(new Date(0));
        document.addAttachment(attachment);

        document.setMetaDataDirty(false);
        document.setContentDirty(false);

        this.oldcore.getSpyXWiki().saveDocument(document, context);

        // Export only the document with the large attachment.
        when(this.documentSelectionResolver.isSelectionSpecified()).thenReturn(true);
        when(this.documentSelectionResolver.getSelectedDocuments()).thenReturn(List.of(docRef));

        // Configure InstanceModel so the export filter can discover and iterate over the test document.
        WikiReference wikiRef = docRef.getWikiReference();
        SpaceReference spaceRef = docRef.getLastSpaceReference();
        when(this.instanceModel.getWikiReferences()).thenReturn(List.of(wikiRef));
        when(this.instanceModel.getSpaceReferences(wikiRef))
            .thenReturn(new EntityReferenceTree(List.of(spaceRef)).getChildren().iterator().next());
        when(this.instanceModel.getDocumentReferences(any(SpaceReference.class)))
            .thenReturn(List.of(docRef));
    }

    /**
     * Verifies that exporting a document with an attachment whose base64-encoded content exceeds 4 GB produces a valid
     * ZIP archive with correct Zip64 size information.
     *
     * <p>This test exposes a bug where {@link ExportAction} writes the XAR directly to the HTTP response output stream
     * (non-seekable), causing {@code ZipArchiveOutputStream} to silently disable Zip64 support and fail with
     * {@code Zip64RequiredException} for entries larger than 4 GB.</p>
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void exportXARWithLargeAttachmentProducesValidZip64Archive() throws Exception
    {
        // Export should complete without error.
        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        // Write response bytes to a temp file so ZipFile can use random access for verification.
        byte[] responseBytes = this.servletOutputStream.baos.toByteArray();
        assertTrue(responseBytes.length > 0, "Export should have produced output");
        File xarFile = new File(this.tempDir, "export.xar");
        Files.write(xarFile.toPath(), responseBytes);

        // Verify the archive is valid and the entry size is correctly stored using Zip64 format.
        try (ZipFile zip = ZipFile.builder().setFile(xarFile).get())
        {
            ZipArchiveEntry docEntry = null;
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements())
            {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().contains("LargeDoc"))
                {
                    docEntry = entry;
                    break;
                }
            }

            assertNotNull(docEntry, "Document entry should exist in archive");
            assertTrue(docEntry.getSize() > 0xFFFFFFFFL,
                "Uncompressed entry size should exceed 4 GB (requires Zip64), was: " + docEntry.getSize());

            // Streaming decompress: verify the full entry can be read and the total matches the recorded size.
            try (InputStream in = zip.getInputStream(docEntry))
            {
                byte[] buffer = new byte[1024 * 1024];
                long totalRead = 0;
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1)
                {
                    totalRead += bytesRead;
                }
                assertEquals(docEntry.getSize(), totalRead,
                    "Decompressed content length should match the recorded entry size");
            }
        }
    }

    /**
     * {@link InputStream} that generates a repeating single-byte pattern without holding data in memory. The resulting
     * stream compresses extremely well under DEFLATE, keeping disk usage minimal despite the large logical size.
     */
    private static class RepetitiveInputStream extends InputStream
    {
        private long remaining;

        private final byte pattern;

        RepetitiveInputStream(long size, byte pattern)
        {
            this.remaining = size;
            this.pattern = pattern;
        }

        @Override
        public int read()
        {
            if (this.remaining <= 0)
            {
                return -1;
            }
            this.remaining--;
            return this.pattern & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len)
        {
            if (this.remaining <= 0)
            {
                return -1;
            }
            int toRead = (int) Math.min(len, this.remaining);
            Arrays.fill(b, off, off + toRead, this.pattern);
            this.remaining -= toRead;
            return toRead;
        }
    }

    /**
     * {@link ServletOutputStream} that captures all written bytes into a {@link ByteArrayOutputStream} for later
     * verification.
     */
    private static final class StubServletOutputStream extends ServletOutputStream
    {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException
        {
            this.baos.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            this.baos.write(b, off, len);
        }

        @Override
        public boolean isReady()
        {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener)
        {
        }
    }
}
