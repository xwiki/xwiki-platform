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
package org.xwiki.rendering.internal.macro.chart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemporaryChartImageWriter}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@ComponentTest
class TemporaryChartImageWriterTest
{
    private static final DocumentReference DATA_DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "data");

    private static final byte[] IMAGE_DATA = new byte[] { 1, 2, 3 };

    @InjectMockComponents
    private TemporaryChartImageWriter writer;

    @MockComponent
    private TemporaryResourceStore temporaryResourceStore;

    @MockComponent
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;

    @TempDir
    private File temporaryDirectory;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.temporaryResourceStore.getTemporaryFile(any())).then(invocation -> {
            TemporaryResourceReference reference = invocation.getArgument(0);
            return new File(this.temporaryDirectory,
                reference.getModuleId() + '/' + reference.getOwningEntityReference().getName() + '/'
                    + reference.getResourceName());
        });
    }

    @Test
    void writeImage() throws Exception
    {
        ImageId imageId = this.writer.writeImage(IMAGE_DATA, DATA_DOCUMENT_REFERENCE);

        assertEquals(DATA_DOCUMENT_REFERENCE, imageId.getDataDocumentReference());
        // The id is a keyed hash of the image content, not a plain hash that could be computed from the content.
        assertEquals(64, imageId.getId().length());
        assertNotEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(IMAGE_DATA)),
            imageId.getId());
        File imageFile = getImageFile(imageId);
        assertArrayEquals(IMAGE_DATA, Files.readAllBytes(imageFile.toPath()));
        // Only the image file is left in the directory.
        assertEquals(1, imageFile.getParentFile().list().length);
    }

    @Test
    void writeIdenticalImage() throws Exception
    {
        ImageId imageId = this.writer.writeImage(IMAGE_DATA, DATA_DOCUMENT_REFERENCE);
        // Change the stored file to verify that it isn't written again.
        Files.write(getImageFile(imageId).toPath(), new byte[] { 4 });

        assertEquals(imageId.getId(), this.writer.writeImage(IMAGE_DATA.clone(), DATA_DOCUMENT_REFERENCE).getId());
        assertArrayEquals(new byte[] { 4 }, Files.readAllBytes(getImageFile(imageId).toPath()));
    }

    @Test
    void writeDifferentImages() throws Exception
    {
        assertNotEquals(this.writer.writeImage(IMAGE_DATA, DATA_DOCUMENT_REFERENCE).getId(),
            this.writer.writeImage(new byte[] { 1, 2, 4 }, DATA_DOCUMENT_REFERENCE).getId());
    }

    @Test
    void writeImageWithError() throws Exception
    {
        IOException exception = new IOException("Failure");
        doThrow(exception).when(this.temporaryResourceStore).getTemporaryFile(any());

        MacroExecutionException thrown = assertThrows(MacroExecutionException.class,
            () -> this.writer.writeImage(IMAGE_DATA, DATA_DOCUMENT_REFERENCE));
        assertEquals("Failed to write the generated chart image", thrown.getMessage());
        assertSame(exception, thrown.getCause());
    }

    @Test
    void getURL() throws Exception
    {
        ExtendedURL url = mock();
        when(url.serialize()).thenReturn("/xwiki/tmp/chart/image.png");
        ArgumentCaptor<ResourceReference> referenceCaptor = ArgumentCaptor.captor();
        when(this.resourceReferenceSerializer.serialize(referenceCaptor.capture())).thenReturn(url);
        ImageId imageId = new ImageId("id", DATA_DOCUMENT_REFERENCE);

        assertEquals("/xwiki/tmp/chart/image.png", this.writer.getURL(imageId));
        TemporaryResourceReference reference = (TemporaryResourceReference) referenceCaptor.getValue();
        assertEquals("chart", reference.getModuleId());
        assertEquals(DATA_DOCUMENT_REFERENCE, reference.getOwningEntityReference());
        assertEquals("id.png", reference.getResourceName());
    }

    private File getImageFile(ImageId imageId) throws IOException
    {
        return this.temporaryResourceStore.getTemporaryFile(
            new TemporaryResourceReference("chart", imageId.getId() + ".png", imageId.getDataDocumentReference()));
    }
}
