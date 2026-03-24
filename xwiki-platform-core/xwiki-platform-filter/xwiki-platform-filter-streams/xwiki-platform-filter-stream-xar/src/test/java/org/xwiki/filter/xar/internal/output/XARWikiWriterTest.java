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
package org.xwiki.filter.xar.internal.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.environment.Environment;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XARWikiWriter}.
 *
 * @version $Id$
 */
@ComponentTest
@ExtendWith(XWikiTempDirExtension.class)
class XARWikiWriterTest
{
    @InjectMockComponents
    private XARWikiWriter writer;

    @MockComponent
    private Environment environment;

    @XWikiTempDir
    private File tempDir;

    @Test
    void smallEntryStaysInMemory() throws Exception
    {
        when(this.environment.getTemporaryDirectory()).thenReturn(this.tempDir);

        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        XAROutputProperties props = new XAROutputProperties();
        props.setTarget(new DefaultOutputStreamOutputTarget(archiveBytes));
        this.writer.initialize("test", props);

        LocalDocumentReference ref = new LocalDocumentReference("Space", "SmallDoc");
        OutputStream entryStream = this.writer.newEntry(ref);
        byte[] content = "<document>small content</document>".getBytes(StandardCharsets.UTF_8);
        entryStream.write(content);
        this.writer.closeEntry();
        this.writer.close();

        // Verify the archive contains the entry with correct content.
        try (ZipArchiveInputStream zis =
            new ZipArchiveInputStream(new ByteArrayInputStream(archiveBytes.toByteArray())))
        {
            ZipArchiveEntry entry = zis.getNextEntry();
            assertNotNull(entry, "Expected Space/SmallDoc.xml entry");
            assertEquals("Space/SmallDoc.xml", entry.getName());

            byte[] readContent = zis.readAllBytes();
            assertArrayEquals(content, readContent, "Entry content should match what was written");

            // Second entry should be package.xml.
            ZipArchiveEntry packageEntry = zis.getNextEntry();
            assertNotNull(packageEntry, "Expected package.xml entry");
            assertEquals("package.xml", packageEntry.getName());
        }
    }

    /**
     * Verifies that writing an entry larger than 4 GB to a non-seekable output stream produces a valid Zip64 archive.
     *
     * <p>This test writes a repeating byte pattern that compresses extremely well, so the archive itself is tiny
     * despite the entry's 4 GB+ uncompressed size. The deferred-file buffering in {@link XARWikiWriter} determines
     * the entry size before adding it to the ZIP, which is the mechanism that enables Zip64 on non-seekable streams.
     * </p>
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void largeEntryProducesValidZip64Archive() throws Exception
    {
        when(this.environment.getTemporaryDirectory()).thenReturn(this.tempDir);

        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        XAROutputProperties props = new XAROutputProperties();
        props.setTarget(new DefaultOutputStreamOutputTarget(archiveBytes));
        this.writer.initialize("test", props);

        // Write an entry whose uncompressed size exceeds the 4 GB Zip64 threshold.
        // A single repeating byte compresses to nearly nothing.
        long targetSize = 4L * 1024 * 1024 * 1024 + 1;
        LocalDocumentReference ref = new LocalDocumentReference("Space", "LargeDoc");
        OutputStream entryStream = this.writer.newEntry(ref);

        byte[] block = new byte[64 * 1024];
        Arrays.fill(block, (byte) 'X');
        long written = 0;
        while (written < targetSize) {
            int toWrite = (int) Math.min(block.length, targetSize - written);
            entryStream.write(block, 0, toWrite);
            written += toWrite;
        }
        this.writer.closeEntry();
        this.writer.close();

        // Verify the archive is valid and contains the entry with the correct Zip64 size.
        try (ZipArchiveInputStream zis =
            new ZipArchiveInputStream(new ByteArrayInputStream(archiveBytes.toByteArray())))
        {
            ZipArchiveEntry entry = zis.getNextEntry();
            assertNotNull(entry, "Expected Space/LargeDoc.xml entry");
            assertEquals("Space/LargeDoc.xml", entry.getName());

            // Streaming-verify: read the entry and check that the byte pattern is correct.
            long totalRead = 0;
            byte[] readBuf = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = zis.read(readBuf)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    if (readBuf[i] != (byte) 'X') {
                        throw new AssertionError(
                            String.format("Unexpected byte %d at position %d", readBuf[i], totalRead + i));
                    }
                }
                totalRead += bytesRead;
            }
            assertEquals(targetSize, totalRead,
                "Total bytes read from archive should match the uncompressed entry size");

            // Verify package.xml follows.
            ZipArchiveEntry packageEntry = zis.getNextEntry();
            assertNotNull(packageEntry, "Expected package.xml entry");
            assertEquals("package.xml", packageEntry.getName());
        }
    }

    @Test
    void multipleEntries() throws Exception
    {
        when(this.environment.getTemporaryDirectory()).thenReturn(this.tempDir);

        ByteArrayOutputStream archiveBytes = new ByteArrayOutputStream();
        XAROutputProperties props = new XAROutputProperties();
        props.setTarget(new DefaultOutputStreamOutputTarget(archiveBytes));
        this.writer.initialize("test", props);

        // Write two entries.
        for (String docName : new String[] {"DocA", "DocB"}) {
            LocalDocumentReference ref = new LocalDocumentReference("MySpace", docName);
            OutputStream entryStream = this.writer.newEntry(ref);
            entryStream.write(("<doc>" + docName + "</doc>").getBytes(StandardCharsets.UTF_8));
            this.writer.closeEntry();
        }
        this.writer.close();

        // Verify both entries plus package.xml.
        int entryCount = 0;
        try (ZipArchiveInputStream zis =
            new ZipArchiveInputStream(new ByteArrayInputStream(archiveBytes.toByteArray())))
        {
            while (zis.getNextEntry() != null) {
                entryCount++;
                // Just consume the entry data.
                zis.readAllBytes();
            }
        }
        // Two document entries + package.xml.
        assertEquals(3, entryCount);
    }
}
