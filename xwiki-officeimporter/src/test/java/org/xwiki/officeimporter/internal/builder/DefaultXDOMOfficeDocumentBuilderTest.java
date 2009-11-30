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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.officeimporter.builder.XDOMOfficeDocumentBuilder;
import org.xwiki.officeimporter.builder.XHTMLOfficeDocumentBuilder;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;

/**
 * Test case for {@link DefaultXDOMOfficeDocumentBuilder}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultXDOMOfficeDocumentBuilderTest extends AbstractOfficeImporterTest
{
    /**
     * The {@link XHTMLOfficeDocumentBuilder} component.
     */
    private XHTMLOfficeDocumentBuilder xhtmlDocumentBuilder;

    /**
     * The {@link XDOMOfficeDocumentBuilder} component.
     */
    private XDOMOfficeDocumentBuilder xdomOfficeDocumentBuilder;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.xhtmlDocumentBuilder = getComponentManager().lookup(XHTMLOfficeDocumentBuilder.class);
        this.xdomOfficeDocumentBuilder = getComponentManager().lookup(XDOMOfficeDocumentBuilder.class);
    }

    /**
     * Test {@link XDOMOfficeDocument} building.
     * 
     * @throws Exception
     */
    @Test
    public void testXDOMOfficeDocumentBuilding() throws Exception
    {
        // Create & register a mock document converter to by-pass openoffice server.
        final byte[] mockInput = new byte[1024];
        final Map<String, byte[]> mockOutput = new HashMap<String, byte[]>();
        mockOutput.put("output.html",
            "<html><head><title></tile></head><body><p><strong>Hello There</strong></p></body></html>".getBytes());

        final OpenOfficeDocumentConverter mockDocumentConverter = this.context.mock(OpenOfficeDocumentConverter.class);
        this.context.checking(new Expectations() {{
                allowing(mockDocumentConverter).convert(mockInput);
                will(returnValue(mockOutput));            
        }});
        ReflectionUtils.setFieldValue(xhtmlDocumentBuilder, "documentConverter", mockDocumentConverter);

        // Create & register a mock document name serializer.
        final DocumentName mockDocumentName = new DocumentName("xwiki", "Main", "Test");
        final DocumentNameSerializer nameSerializer = this.context.mock(DocumentNameSerializer.class, "test");
        this.context.checking(new Expectations() {{
                allowing(nameSerializer).serialize(mockDocumentName);
                will(returnValue("xwiki:Main.Test"));            
        }});
        ReflectionUtils.setFieldValue(xhtmlDocumentBuilder, "nameSerializer", nameSerializer);

        XDOMOfficeDocument document = xdomOfficeDocumentBuilder.build(mockInput, mockDocumentName, true);
        Assert.assertNotNull(document.getContentDocument());
        Assert.assertEquals("**Hello There**", document.getContentAsString());
        Assert.assertEquals(0, document.getArtifacts().size());
    }
}
