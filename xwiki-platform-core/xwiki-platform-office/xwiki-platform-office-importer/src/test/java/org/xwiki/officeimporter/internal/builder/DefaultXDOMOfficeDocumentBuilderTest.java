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
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;

/**
 * Test case for {@link DefaultXDOMOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultXDOMOfficeDocumentBuilderTest extends AbstractOfficeImporterTest
{
    /**
     * The name of an input file to be used in tests.
     */
    private static final String INPUT_FILE_NAME = "office.doc";

    /**
     * The name of the output file corresponding to {@link #INPUT_FILE_NAME}.
     */
    private static final String OUTPUT_FILE_NAME = "office.html";

    /**
     * The {@link XDOMOfficeDocumentBuilder} component.
     */
    private XDOMOfficeDocumentBuilder xdomOfficeDocumentBuilder;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.xdomOfficeDocumentBuilder = getComponentManager().getInstance(XDOMOfficeDocumentBuilder.class);
    }

    /**
     * Test {@link XDOMOfficeDocument} building.
     */
    @org.junit.Test
    public void testXDOMOfficeDocumentBuilding()
    {
        // Create & register a mock document converter to by-pass openoffice server.
        final InputStream mockOfficeFileStream = new ByteArrayInputStream(new byte[1024]);
        final Map<String, InputStream> mockInput = new HashMap<String, InputStream>();
        mockInput.put(INPUT_FILE_NAME, mockOfficeFileStream);
        final Map<String, byte[]> mockOutput = new HashMap<String, byte[]>();
        mockOutput.put(OUTPUT_FILE_NAME,
            "<html><head><title></tile></head><body><p><strong>Hello There</strong></p></body></html>".getBytes());

        final OpenOfficeConverter mockDocumentConverter = getMockery().mock(OpenOfficeConverter.class);
        final DocumentReference documentReference = new DocumentReference("xwiki", "Main", "Test");

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockOpenOfficeManager).getConverter();
                will(returnValue(mockDocumentConverter));

                try {
                    allowing(mockDocumentConverter).convert(mockInput, INPUT_FILE_NAME, OUTPUT_FILE_NAME);
                    will(returnValue(mockOutput));
                } catch (OpenOfficeConverterException e) {
                    Assert.fail(e.getMessage());
                }

                allowing(mockDefaultStringEntityReferenceSerializer).serialize(documentReference);
                will(returnValue("xwiki:Main.Test"));
            }
        });

        XDOMOfficeDocument document = null;
        try {
            document = xdomOfficeDocumentBuilder.build(mockOfficeFileStream, INPUT_FILE_NAME, documentReference, true);
        } catch (OfficeImporterException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(document.getContentDocument());
        Assert.assertEquals("**Hello There**", document.getContentAsString());
        Assert.assertEquals(0, document.getArtifacts().size());
    }
}
