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
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XHTMLOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;

/**
 * Test case for {@link DefaultXHTMLOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultXHTMLOfficeDocumentBuilderTest extends AbstractOfficeImporterTest
{
    /**
     * The {@link XHTMLOfficeDocumentBuilder} component.
     */
    private XHTMLOfficeDocumentBuilder xhtmlDocumentBuilder;
    
    /**
     * Used to setup a mock document converter.
     */
    private OpenOfficeManager officeManager;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.xhtmlDocumentBuilder = getComponentManager().lookup(XHTMLOfficeDocumentBuilder.class);
        this.officeManager = getComponentManager().lookup(OpenOfficeManager.class);        
    }

    /**
     * Tests {@link XHTMLOfficeDocument} building.
     * 
     * @throws Exception
     */
    @org.junit.Test
    public void testXHTMLOfficeDocumentBuilding() throws Exception
    {
        // Create & register a mock document converter to by-pass openoffice server.
        final InputStream mockOfficeFileStream = new ByteArrayInputStream(new byte[1024]);
        final Map<String, InputStream> mockInput = new HashMap<String, InputStream>();
        mockInput.put("input.doc", mockOfficeFileStream);
        final Map<String, byte[]> mockOutput = new HashMap<String, byte[]>();
        mockOutput.put("output.html", "<html><head><title></tile></head><body></body></html>".getBytes());

        final OpenOfficeConverter mockDocumentConverter = this.mockery.mock(OpenOfficeConverter.class);
        this.mockery.checking(new Expectations() {{
            oneOf(mockOpenOfficeManager).getConverter();
            will(returnValue(mockDocumentConverter));
            allowing(mockDocumentConverter).convert(mockInput, "input.doc", "output.html");
            will(returnValue(mockOutput));
        }});
        ReflectionUtils.setFieldValue(officeManager, "converter", mockDocumentConverter);

        // Create & register a mock entity reference serializer.
        final DocumentReference documentReference = new DocumentReference("xwiki", "Main", "Test");
        this.mockery.checking(new Expectations() {{
            allowing(mockDefaultStringEntityReferenceSerializer).serialize(documentReference);
            will(returnValue("xwiki:Main.Test"));
        }});

        XHTMLOfficeDocument document =
            xhtmlDocumentBuilder.build(mockOfficeFileStream, "input.doc", documentReference, true);
        Assert.assertNotNull(document.getContentDocument());
        Assert.assertEquals(0, document.getArtifacts().size());
    }
}
