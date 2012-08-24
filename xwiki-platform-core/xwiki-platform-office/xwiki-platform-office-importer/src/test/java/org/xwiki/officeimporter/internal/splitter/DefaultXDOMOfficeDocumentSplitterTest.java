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
package org.xwiki.officeimporter.internal.splitter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;

/**
 * Test case for {@link DefaultXDOMOfficeDocumentSplitter}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
public class DefaultXDOMOfficeDocumentSplitterTest extends AbstractOfficeImporterTest
{
    /**
     * Parser for building XDOM instances.
     */
    private Parser xwikiSyntaxParser;

    /**
     * Document splitter for testing.
     */
    private XDOMOfficeDocumentSplitter officeDocumentSplitter;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.xwikiSyntaxParser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");
        this.officeDocumentSplitter = getComponentManager().getInstance(XDOMOfficeDocumentSplitter.class);
    }

    /**
     * Test basic document splitting.
     * 
     * @throws Exception if it fails to parse the wiki syntax or if it fails to split the document
     */
    @Test
    public void testDocumentSplitting() throws Exception
    {
        // Create xwiki/2.0 document.
        StringBuffer buffer = new StringBuffer();
        buffer.append("=Heading1=").append('\n');
        buffer.append("Content").append('\n');
        buffer.append("==Heading11==").append('\n');
        buffer.append("Content").append('\n');
        buffer.append("==Heading12==").append('\n');
        buffer.append("Content").append('\n');
        buffer.append("=Heading2=").append('\n');
        buffer.append("Content").append('\n');
        XDOM xdom = xwikiSyntaxParser.parse(new StringReader(buffer.toString()));

        // Create xdom office document.
        XDOMOfficeDocument officeDocument =
            new XDOMOfficeDocument(xdom, new HashMap<String, byte[]>(), getComponentManager());
        final DocumentReference baseDocument = new DocumentReference("xwiki", "Test", "Test");

        // Add expectations to mock document name serializer.
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockCompactWikiStringEntityReferenceSerializer).serialize(baseDocument);
                will(returnValue("Test.Test"));
                allowing(mockCompactWikiStringEntityReferenceSerializer).serialize(
                    new DocumentReference("xwiki", "Test", "Heading1"));
                will(returnValue("Test.Heading1"));
                allowing(mockCompactWikiStringEntityReferenceSerializer).serialize(
                    new DocumentReference("xwiki", "Test", "Heading11"));
                will(returnValue("Test.Heading11"));
                allowing(mockCompactWikiStringEntityReferenceSerializer).serialize(
                    new DocumentReference("xwiki", "Test", "Heading12"));
                will(returnValue("Test.Heading12"));
                allowing(mockCompactWikiStringEntityReferenceSerializer).serialize(
                    new DocumentReference("xwiki", "Test", "Heading2"));
                will(returnValue("Test.Heading2"));
            }
        });

        // Add expectations to mock document name factory.
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentReferenceResolver).resolve("Test.Test");
                will(returnValue(new DocumentReference("xwiki", "Test", "Test")));
                allowing(mockDocumentReferenceResolver).resolve("Test.Heading1");
                will(returnValue(new DocumentReference("xwiki", "Test", "Heading1")));
                allowing(mockDocumentReferenceResolver).resolve("Test.Heading11");
                will(returnValue(new DocumentReference("xwiki", "Test", "Heading11")));
                allowing(mockDocumentReferenceResolver).resolve("Test.Heading12");
                will(returnValue(new DocumentReference("xwiki", "Test", "Heading12")));
                allowing(mockDocumentReferenceResolver).resolve("Test.Heading2");
                will(returnValue(new DocumentReference("xwiki", "Test", "Heading2")));
            }
        });

        // Add expectations to mock document access bridge.
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentAccessBridge).exists("Test.Heading1");
                will(returnValue(false));
                allowing(mockDocumentAccessBridge).exists("Test.Heading11");
                will(returnValue(false));
                allowing(mockDocumentAccessBridge).exists("Test.Heading12");
                will(returnValue(false));
                allowing(mockDocumentAccessBridge).exists("Test.Heading2");
                will(returnValue(false));
            }
        });

        // Perform the split operation.
        Map<TargetDocumentDescriptor, XDOMOfficeDocument> result =
            officeDocumentSplitter.split(officeDocument, new int[] {1, 2, 3, 4, 5, 6}, "headingNames", baseDocument);

        // There should be five XDOM office documents.
        Assert.assertEquals(5, result.size());
    }
}
