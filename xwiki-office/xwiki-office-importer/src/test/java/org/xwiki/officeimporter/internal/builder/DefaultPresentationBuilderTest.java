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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.PresentationBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

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
     * The {@link PresentationBuilder} component.
     */
    private PresentationBuilder presentationBuilder;

    /**
     * The component used to parse the presentation HTML.
     */
    private Parser mockXHTMLParser;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.presentationBuilder = getComponentManager().lookup(PresentationBuilder.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractOfficeImporterTest#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        mockXHTMLParser = registerMockComponent(Parser.class, "xhtml/1.0");
    }

    /**
     * Test presentation {@link XDOMOfficeDocument} building.
     */
    @org.junit.Test
    public void testPresentationBuilding()
    {
        // Create & register a mock document converter to by-pass OpenOffice server.
        final InputStream mockOfficeFileStream = new ByteArrayInputStream(new byte[1024]);
        final Map<String, InputStream> mockInput = new HashMap<String, InputStream>();
        mockInput.put(INPUT_FILE_NAME, mockOfficeFileStream);
        final Map<String, byte[]> mockOutput = new HashMap<String, byte[]>();
        mockOutput.put("img0.jpg", new byte[0]);
        mockOutput.put(OUTPUT_FILE_NAME, new byte[0]);
        mockOutput.put("text0.html", new byte[0]);
        mockOutput.put("img1.jpg", new byte[0]);
        mockOutput.put("img1.html", new byte[0]);
        mockOutput.put("text1.html", new byte[0]);

        final OpenOfficeConverter mockDocumentConverter = getMockery().mock(OpenOfficeConverter.class);
        final DocumentReference reference = new DocumentReference("xwiki", "Main", "Test");
        final String stringReference = "xwiki:Main.Test";
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockOpenOfficeManager).getConverter();
                will(returnValue(mockDocumentConverter));

                try {
                    oneOf(mockDocumentConverter).convert(mockInput, INPUT_FILE_NAME, OUTPUT_FILE_NAME);
                    will(returnValue(mockOutput));
                } catch (OpenOfficeConverterException e) {
                    Assert.fail(e.getMessage());
                }

                oneOf(mockDefaultStringEntityReferenceSerializer).serialize(reference);
                will(returnValue(stringReference));
            }
        });

        final AttachmentReference firstImageReference = new AttachmentReference("office-slide0.jpg", reference);
        final AttachmentReference secondImageReference = new AttachmentReference("office-slide1.jpg", reference);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockDocumentReferenceResolver).resolve(stringReference);
                will(returnValue(reference));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(firstImageReference, false);
                will(returnValue("/xwiki/bin/download/Main/Test/office-slide0.jpg"));

                oneOf(mockDocumentAccessBridge).getAttachmentURL(secondImageReference, false);
                will(returnValue("/xwiki/bin/download/Main/Test/office-slide1.jpg"));

                try {
                    oneOf(mockXHTMLParser).parse(with(aNonNull(StringReader.class)));
                    will(returnValue(new XDOM(Arrays.asList(new Block[] {}))));
                } catch (ParseException e) {
                    Assert.fail(e.getMessage());
                }
            }
        });

        XDOMOfficeDocument presentation = null;
        try {
            presentation = presentationBuilder.build(mockOfficeFileStream, INPUT_FILE_NAME, reference);
        } catch (OfficeImporterException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(presentation.getContentDocument());
        Assert.assertEquals(2, presentation.getArtifacts().size());
        Assert.assertTrue(presentation.getArtifacts().containsKey(firstImageReference.getName()));
        Assert.assertTrue(presentation.getArtifacts().containsKey(secondImageReference.getName()));
    }
}
