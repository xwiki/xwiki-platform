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
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test case for {@link DefaultXHTMLOfficeDocumentBuilder}.
 *
 * @version $Id$
 * @since 2.1M1
 */
@ComponentTest
public class DefaultXHTMLOfficeDocumentBuilderTest
{
    @InjectMockComponents
    private DefaultXHTMLOfficeDocumentBuilder officeDocumentBuilder;

    @MockComponent
    private OfficeConverter officeConverter;

    @MockComponent
    @Named("openoffice")
    private HTMLCleaner officeHTMLCleaner;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private OfficeServer officeServer;

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.officeServer.getConverter()).thenReturn(this.officeConverter);
    }

    @Test
    public void xhtmlOfficeDocumentBuilding() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:Path.To.Page");

        InputStream officeFileStream = new ByteArrayInputStream("office content".getBytes());
        Map<String, byte[]> artifacts = new HashMap<String, byte[]>();
        artifacts.put("file.html", "HTML content".getBytes());
        artifacts.put("file.txt", "Text content".getBytes());
        when(this.officeConverter.convert(Collections.singletonMap("file.odt", officeFileStream), "file.odt",
            "file.html")).thenReturn(artifacts);

        Map<String, byte[]> embeddedImages = Collections.singletonMap("image.png", "Image content".getBytes());
        Document xhtmlDoc = mock(Document.class);
        when(xhtmlDoc.getUserData("embeddedImages")).thenReturn(embeddedImages);

        HTMLCleanerConfiguration config = mock(HTMLCleanerConfiguration.class);
        when(this.officeHTMLCleaner.getDefaultConfiguration()).thenReturn(config);
        when(this.officeHTMLCleaner.clean(any(Reader.class), eq(config))).thenReturn(xhtmlDoc);

        XHTMLOfficeDocument result =
            this.officeDocumentBuilder.build(officeFileStream, "file.odt", documentReference, true);

        Map<String, String> params = new HashMap<String, String>();
        params.put("targetDocument", "wiki:Path.To.Page");
        params.put("attachEmbeddedImages", "true");
        params.put("filterStyles", "strict");
        verify(config).setParameters(params);

        assertEquals(xhtmlDoc, result.getContentDocument());

        Map<String, byte[]> expectedArtifacts = new HashMap<String, byte[]>();
        expectedArtifacts.put("file.txt", artifacts.get("file.txt"));
        expectedArtifacts.put("image.png", embeddedImages.get("image.png"));
        assertEquals(expectedArtifacts, result.getArtifacts());
    }
}
