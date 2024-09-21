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
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.document.OfficeDocumentArtifact;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.document.FileOfficeDocumentArtifact;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultPresentationBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@ComponentTest
class DefaultPresentationBuilderTest
{
    @InjectMockComponents
    private DefaultPresentationBuilder presentationBuilder;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    /**
     * The component used to parse the presentation HTML.
     */
    @MockComponent
    @Named("xhtml/1.0")
    private Parser xhtmlParser;

    @MockComponent
    private OfficeConverter officeConverter;

    @MockComponent
    @Named("openoffice")
    private HTMLCleaner officeHTMLCleaner;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private OfficeServer officeServer;

    @XWikiTempDir
    private File outputDirectory;

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.officeServer.getConverter()).thenReturn(this.officeConverter);
    }

    @Test
    void build() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:Path.To.Page");

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentAccessBridge dab = this.componentManager.getInstance(DocumentAccessBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(document);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        InputStream officeFileStream = new ByteArrayInputStream("Presentation content".getBytes());

        OfficeConverterResult officeConverterResult = mock(OfficeConverterResult.class);
        when(officeConverterResult.getOutputDirectory()).thenReturn(this.outputDirectory);

        // Copy the pdf file to the output directory
        File outputFile = new File(this.outputDirectory, "presentation.pdf");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/pdf.pdf")),
            outputFile.toPath());

        when(officeConverterResult.getOutputFile()).thenReturn(outputFile);

        when(officeConverterResult.getAllFiles()).thenReturn(Set.of(outputFile));

        when(this.officeConverter.convertDocument(Collections.singletonMap("input.odp", officeFileStream), "input.odp",
            "presentation.pdf")).thenReturn(officeConverterResult);

        HTMLCleanerConfiguration config = mock(HTMLCleanerConfiguration.class);
        when(this.officeHTMLCleaner.getDefaultConfiguration()).thenReturn(config);

        Document xhtmlDoc = XMLUtils.createDOMDocument();
        xhtmlDoc.appendChild(xhtmlDoc.createElement("html"));
        String presentationHTML = Stream.of(0).map(slideNumber ->
            String.format("<p><img src=\"file-slide%d.png\"/></p>", slideNumber)).collect(Collectors.joining());
        when(this.officeHTMLCleaner.clean(any(), eq(config)))
            .then(returnMatchingDocument(presentationHTML, xhtmlDoc));

        XDOM galleryContent = new XDOM(Collections.<Block>emptyList());
        when(this.xhtmlParser.parse(any(Reader.class))).thenReturn(galleryContent);

        XDOMOfficeDocument result = this.presentationBuilder.build(officeFileStream, "file.odp", documentReference);

        verify(config).setParameters(Collections.singletonMap("targetDocument", "wiki:Path.To.Page"));
        Map<String, OfficeDocumentArtifact> expectedArtifacts = Stream.of(0)
            .map(slideNumber -> new FileOfficeDocumentArtifact(String.format("file-slide%d.png", slideNumber),
                new File(this.outputDirectory, String.format("slide%d.png", slideNumber))))
            .collect(Collectors.toMap(OfficeDocumentArtifact::getName, Function.identity()));
        assertEquals(expectedArtifacts, result.getArtifactsMap());

        assertEquals("wiki:Path.To.Page", result.getContentDocument().getMetaData().getMetaData(MetaData.BASE));

        List<ExpandedMacroBlock> macros =
            result.getContentDocument().getBlocks(new ClassBlockMatcher(ExpandedMacroBlock.class), Block.Axes.CHILD);
        assertEquals(1, macros.size());
        assertEquals("gallery", macros.get(0).getId());
        assertEquals(galleryContent, macros.get(0).getChildren().get(0));
    }

    private Answer<Document> returnMatchingDocument(final String content, final Document document)
    {
        return new Answer<Document>()
        {
            @Override
            public Document answer(InvocationOnMock invocation) throws Throwable
            {
                Reader reader = invocation.getArgument(0);
                return StringUtils.equals(content, IOUtils.toString(reader)) ? document : null;
            }
        };
    }
}
