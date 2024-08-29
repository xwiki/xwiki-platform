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
package org.xwiki.officeimporter.internal.converter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.job.ConversionJobWithOptionalSourceFormatUnspecified;
import org.jodconverter.core.job.ConversionJobWithOptionalTargetFormatUnspecified;
import org.jodconverter.core.office.OfficeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.XWikiTempDirExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultOfficeConverter}.
 *
 * @version $Id$
 */
@ExtendWith(XWikiTempDirExtension.class)
class DefaultOfficeConverterTest
{
    private DocumentConverter localConverter;

    @XWikiTempDir
    private File tmpDir;

    private DefaultOfficeConverter defaultOfficeConverter;

    @BeforeEach
    void setup()
    {
        this.localConverter = mock(DocumentConverter.class);
        this.defaultOfficeConverter = new DefaultOfficeConverter(localConverter, tmpDir);
    }

    @Test
    void convertDocument() throws IOException, OfficeException, OfficeConverterException
    {
        OfficeConverterException officeConverterException = assertThrows(OfficeConverterException.class,
            () -> this.defaultOfficeConverter.convertDocument(Collections.emptyMap(), "myFile", "myOutputFile"));
        assertEquals("No input stream specified for main input file [myFile].", officeConverterException.getMessage());

        String content = "my content";

        Map<String, InputStream> inputStreamMap = Collections.singletonMap("myFile",
            new ByteArrayInputStream(content.getBytes()));

        ConversionJobWithOptionalSourceFormatUnspecified jobSource =
            mock(ConversionJobWithOptionalSourceFormatUnspecified.class);
        when(this.localConverter.convert(any(File.class))).thenReturn(jobSource);

        ConversionJobWithOptionalTargetFormatUnspecified jobTarget =
            mock(ConversionJobWithOptionalTargetFormatUnspecified.class);
        when(jobSource.to(any(File.class))).thenAnswer(invocationOnMock -> {
            // We create the output file and a mock artifact file next to it.
            File outputFile = invocationOnMock.getArgument(0);
            Files.createFile(outputFile.toPath());
            File otherArtifact = new File(outputFile.getParentFile(), "artifact.test");
            Files.createFile(otherArtifact.toPath());
            return jobTarget;
        });

        OfficeConverterResult result =
            this.defaultOfficeConverter.convertDocument(inputStreamMap, "myFile", "myOutputFile");

        assertEquals("myOutputFile", result.getOutputFile().getName());
        File outputDirectory = result.getOutputDirectory();

        // path of the outputDirectory is: tmpDir / UUID / output
        // We cannot guess the UUID so we're checking the other parts
        assertEquals("output", outputDirectory.getName());
        assertEquals(this.tmpDir, outputDirectory.getParentFile().getParentFile());
        Set<File> allFiles = result.getAllFiles();
        assertEquals(2, allFiles.size());
        assertTrue(allFiles.contains(new File(outputDirectory, "myOutputFile")));
        assertTrue(allFiles.contains(new File(outputDirectory, "artifact.test")));

        ArgumentCaptor<File> argument = ArgumentCaptor.forClass(File.class);
        verify(this.localConverter).convert(argument.capture());
        assertEquals("myFile", argument.getValue().getName());

        argument = ArgumentCaptor.forClass(File.class);
        verify(jobSource).to(argument.capture());
        assertEquals("myOutputFile", argument.getValue().getName());

        verify(jobTarget).execute();
    }

    @Test
    void isConversionSupported() throws Exception
    {
        when(this.localConverter.getFormatRegistry()).thenReturn(DefaultDocumentFormatRegistry.getInstance());
        for (String mediaType : Arrays.asList("application/vnd.oasis.opendocument.text", "application/msword",
            "application/vnd.oasis.opendocument.presentation", "application/vnd.ms-powerpoint",
            "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.ms-excel", "text/html")) {
            assertTrue(this.defaultOfficeConverter.isConversionSupported(mediaType, "text/html"),
                String.format("%s conversion to text/html not supported", mediaType));
        }
        for (String mediaType : Arrays.asList("foo/bar", "application/pdf")) {
            assertFalse(this.defaultOfficeConverter.isConversionSupported(mediaType, "text/html"),
                String.format("%s conversion to text/html supported while it shouldn't", mediaType));
        }
    }

    @Test
    void getDocumentFormat()
    {
        when(this.localConverter.getFormatRegistry()).thenReturn(DefaultDocumentFormatRegistry.getInstance());
        assertNull(this.defaultOfficeConverter.getDocumentFormat("test.foo"));
        assertEquals("odt", this.defaultOfficeConverter.getDocumentFormat("test.odt").getExtension());
    }
}
