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
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ExpandedMacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Test case for {@link DefaultPresentationBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultPresentationBuilderTest extends AbstractOfficeImporterTest
{
    /**
     * The name of an input file to be used in tests.
     */
    private static final String INPUT_FILE_NAME = "office.ppt";

    /**
     * The name of the output file.
     */
    private static final String OUTPUT_FILE_NAME = "img0.html";

    /**
     * The {@link PresentationBuilder} component being tested.
     */
    private DefaultPresentationBuilder presentationBuilder;

    /**
     * The component used to parse the presentation HTML.
     */
    private Parser mockXHTMLParser;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        presentationBuilder = (DefaultPresentationBuilder) getComponentManager().getInstance(PresentationBuilder.class);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        mockXHTMLParser = registerMockComponent(Parser.class, "xhtml/1.0");
    }

    /**
     * Tests {@link DefaultPresentationBuilder#importPresentation(InputStream, String)}.
     */
    @Test
    public void testImportPresentation()
    {
        InputStream officeFileStream = new ByteArrayInputStream(new byte[1024]);
        final Map<String, InputStream> input = Collections.singletonMap(INPUT_FILE_NAME, officeFileStream);
        final Map<String, byte[]> output = Collections.singletonMap(OUTPUT_FILE_NAME, new byte[0]);
        final OpenOfficeConverter mockDocumentConverter = getMockery().mock(OpenOfficeConverter.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockOpenOfficeManager).getConverter();
                will(returnValue(mockDocumentConverter));

                try {
                    oneOf(mockDocumentConverter).convert(input, INPUT_FILE_NAME, OUTPUT_FILE_NAME);
                    will(returnValue(output));
                } catch (OpenOfficeConverterException e) {
                    Assert.fail(e.getMessage());
                }
            }
        });

        try {
            Assert.assertEquals(output, presentationBuilder.importPresentation(officeFileStream, INPUT_FILE_NAME));
        } catch (OfficeImporterException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests {@link DefaultPresentationBuilder#buildPresentationHTML(Map, String)}.
     */
    @Test
    public void testBuildPresentationHTML()
    {
        final Map<String, byte[]> artifacts = new HashMap<String, byte[]>();
        artifacts.put("img0.jpg", new byte[0]);
        artifacts.put(OUTPUT_FILE_NAME, new byte[0]);
        artifacts.put("text0.html", new byte[0]);
        artifacts.put("img1.jpg", new byte[0]);
        artifacts.put("img1.html", new byte[0]);
        artifacts.put("text1.html", new byte[0]);

        Assert.assertEquals("<p><img src=\"test-slide0.jpg\"/></p><p><img src=\"test-slide1.jpg\"/></p>",
            presentationBuilder.buildPresentationHTML(artifacts, "test"));
        Assert.assertEquals(2, artifacts.size());
        Assert.assertTrue(artifacts.containsKey("test-slide0.jpg"));
        Assert.assertTrue(artifacts.containsKey("test-slide1.jpg"));
    }

    /**
     * Tests {@link DefaultPresentationBuilder#cleanPresentationHTML(String, DocumentReference)}.
     */
    @Test
    public void testCleanPresentationHTML()
    {
        final DocumentReference reference = new DocumentReference("xwiki", "Main", "Test");
        final String stringReference = "xwiki:Main.Test";
        final AttachmentReference imageReference = new AttachmentReference("office-slide0.jpg", reference);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDefaultStringEntityReferenceSerializer).serialize(reference);
                will(returnValue(stringReference));

                oneOf(mockDocumentReferenceResolver).resolve(stringReference);
                will(returnValue(reference));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(imageReference, false);
                will(returnValue("/xwiki/bin/download/Main/Test/office-slide0.jpg"));
            }
        });

        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html><p><!--startimage:false|-|attach|-|office-slide0.jpg-->"
            + "<img src=\"/xwiki/bin/download/Main/Test/office-slide0.jpg\"></img><!--stopimage--></p></html>\n",
            presentationBuilder.cleanPresentationHTML("<p><img src=\"office-slide0.jpg\"/></p>", reference));
    }

    /**
     * Tests {@link DefaultPresentationBuilder#buildPresentationXDOM(String, DocumentReference)}.
     */
    @Test
    public void testBuildPresentationXDOM()
    {
        final DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        final DocumentModelBridge mockDocumentModelBridge = getMockery().mock(DocumentModelBridge.class);
        final XDOM galleryContent = new XDOM(Collections.<Block> emptyList());
        getMockery().checking(new Expectations()
        {
            {
                try {
                    oneOf(mockDocumentAccessBridge).getDocument(reference);
                    will(returnValue(mockDocumentModelBridge));
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }

                oneOf(mockDocumentModelBridge).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));

                try {
                    oneOf(mockXHTMLParser).parse(with(aNonNull(StringReader.class)));
                    will(returnValue(galleryContent));
                } catch (ParseException e) {
                    Assert.fail(e.getMessage());
                }
            }
        });

        XDOM xdom = null;
        try {
            xdom = presentationBuilder.buildPresentationXDOM("some HTML", reference);
        } catch (OfficeImporterException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(xdom);

        List<ExpandedMacroBlock> macros =
            xdom.getBlocks(new ClassBlockMatcher(ExpandedMacroBlock.class), Block.Axes.CHILD);
        Assert.assertEquals(1, macros.size());
        Assert.assertEquals("gallery", macros.get(0).getId());
        Assert.assertEquals(galleryContent, macros.get(0).getChildren().get(0));
    }
}
