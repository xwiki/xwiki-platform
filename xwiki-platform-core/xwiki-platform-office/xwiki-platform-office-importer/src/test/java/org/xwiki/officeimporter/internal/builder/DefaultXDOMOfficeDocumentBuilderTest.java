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
package org.xwiki.officeimporter.internal.builder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.htmlcleaner.HtmlCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.OfficeDocument;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultXDOMOfficeDocumentBuilder}.
 *
 * @version $Id$
 * @since 2.1M1
 */
@ComponentTest
class DefaultXDOMOfficeDocumentBuilderTest extends AbstractOfficeImporterTest
{
    /**
     * The name of an input file to be used in tests.
     */
    private static final String INPUT_FILE_NAME = "office.doc";

    /**
     * The name of the output file corresponding to {@link #INPUT_FILE_NAME}.
     */
    private static final String OUTPUT_FILE_NAME = "office.html";

    @XWikiTempDir
    private File outputDirectory;

    @MockComponent
    @Named("openoffice")
    private HtmlCleaner htmlCleaner;

    /**
     * The {@link XDOMOfficeDocumentBuilder} component.
     */
    private XDOMOfficeDocumentBuilder xdomOfficeDocumentBuilder;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.xdomOfficeDocumentBuilder = this.componentManager.getInstance(XDOMOfficeDocumentBuilder.class);
    }

    /**
     * Test {@link OfficeDocument} building.
     */
    @Test
    void xdomOfficeDocumentBuilding() throws Exception
    {
        // Create & register a mock document converter to by-pass the office server.
        final InputStream mockOfficeFileStream = new ByteArrayInputStream(new byte[1024]);
        final Map<String, InputStream> mockInput = new HashMap<>();
        String internalInputFilename = "input.doc";
        mockInput.put(internalInputFilename, mockOfficeFileStream);
        OfficeConverterResult converterResult = mock(OfficeConverterResult.class);
        when(converterResult.getOutputDirectory()).thenReturn(this.outputDirectory);
        File outputFile = new File(this.outputDirectory, OUTPUT_FILE_NAME);
        when(converterResult.getOutputFile()).thenReturn(outputFile);
        Files.createFile(outputFile.toPath());
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            IOUtils.write(
                "<html><head><title></tile></head><body><p><strong>Hello There</strong></p></body></html>".getBytes(),
                fos);
        }

        final OfficeConverter mockDocumentConverter = mock(OfficeConverter.class);
        final DocumentReference documentReference = new DocumentReference("xwiki", "Main", "Test");

        when(mockOfficeServer.getConverter()).thenReturn(mockDocumentConverter);
        when(mockDocumentConverter.convertDocument(mockInput, internalInputFilename, "output.html"))
            .thenReturn(converterResult);
        when(mockDocumentReferenceResolver.resolve("xwiki:Main.Test")).thenReturn(documentReference);
        when(mockDefaultStringEntityReferenceSerializer.serialize(documentReference)).thenReturn("xwiki:Main.Test");

        XDOMOfficeDocument document =
            xdomOfficeDocumentBuilder.build(mockOfficeFileStream, INPUT_FILE_NAME, documentReference, true);
        assertEquals("xwiki:Main.Test", document.getContentDocument().getMetaData().getMetaData(MetaData.BASE));
        assertEquals("**Hello There**", document.getContentAsString());
        assertEquals(0, document.getArtifactsMap().size());

        verify(mockOfficeServer).getConverter();
    }
}
