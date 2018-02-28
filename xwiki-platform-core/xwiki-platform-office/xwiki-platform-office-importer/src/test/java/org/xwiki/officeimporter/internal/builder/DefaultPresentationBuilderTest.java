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
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test case for {@link DefaultPresentationBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultPresentationBuilderTest
{
    @Rule
    public MockitoComponentMockingRule<PresentationBuilder> mocker =
        new MockitoComponentMockingRule<PresentationBuilder>(DefaultPresentationBuilder.class);

    /**
     * The component used to parse the presentation HTML.
     */
    private Parser xhtmlParser;

    private OfficeConverter officeConverter;

    private HTMLCleaner officeHTMLCleaner;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Before
    public void configure() throws Exception
    {
        this.xhtmlParser = this.mocker.getInstance(Parser.class, "xhtml/1.0");
        this.officeHTMLCleaner = this.mocker.getInstance(HTMLCleaner.class, "openoffice");
        this.entityReferenceSerializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);

        this.officeConverter = mock(OfficeConverter.class);
        OfficeServer officeServer = this.mocker.getInstance(OfficeServer.class);
        when(officeServer.getConverter()).thenReturn(this.officeConverter);
    }

    @Test
    public void build() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn("wiki:Path.To.Page");

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getTranslatedDocumentInstance(documentReference)).thenReturn(document);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        InputStream officeFileStream = new ByteArrayInputStream("Presentation content".getBytes());
        Map<String, byte[]> artifacts = new HashMap<String, byte[]>();
        byte[] firstSlide = "first slide".getBytes();
        byte[] secondSlide = "second slide".getBytes();
        artifacts.put("img0.jpg", firstSlide);
        artifacts.put("img0.html", new byte[0]);
        artifacts.put("text0.html", new byte[0]);
        artifacts.put("img1.jpg", secondSlide);
        artifacts.put("img1.html", new byte[0]);
        artifacts.put("text1.html", new byte[0]);
        when(this.officeConverter.convert(Collections.singletonMap("file.odp", officeFileStream), "file.odp",
            "img0.html")).thenReturn(artifacts);

        HTMLCleanerConfiguration config = mock(HTMLCleanerConfiguration.class);
        when(this.officeHTMLCleaner.getDefaultConfiguration()).thenReturn(config);

        Document xhtmlDoc = XMLUtils.createDOMDocument();
        xhtmlDoc.appendChild(xhtmlDoc.createElement("html"));
        String presentationHTML = "<p><img src=\"file-slide0.jpg\"/></p><p><img src=\"file-slide1.jpg\"/></p>";
        when(this.officeHTMLCleaner.clean(any(Reader.class), eq(config)))
            .then(returnMatchingDocument(presentationHTML, xhtmlDoc));

        XDOM galleryContent = new XDOM(Collections.<Block>emptyList());
        when(this.xhtmlParser.parse(any(Reader.class))).thenReturn(galleryContent);

        XDOMOfficeDocument result =
            this.mocker.getComponentUnderTest().build(officeFileStream, "file.odp", documentReference);

        verify(config).setParameters(Collections.singletonMap("targetDocument", "wiki:Path.To.Page"));

        Map<String, byte[]> expectedArtifacts = new HashMap<String, byte[]>();
        expectedArtifacts.put("file-slide0.jpg", firstSlide);
        expectedArtifacts.put("file-slide1.jpg", secondSlide);
        assertEquals(expectedArtifacts, result.getArtifacts());

        assertEquals("wiki:Path.To.Page", result.getContentDocument().getMetaData().getMetaData(MetaData.BASE));

        List<ExpandedMacroBlock> macros =
            result.getContentDocument().getBlocks(new ClassBlockMatcher(ExpandedMacroBlock.class), Block.Axes.CHILD);
        Assert.assertEquals(1, macros.size());
        Assert.assertEquals("gallery", macros.get(0).getId());
        Assert.assertEquals(galleryContent, macros.get(0).getChildren().get(0));
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
