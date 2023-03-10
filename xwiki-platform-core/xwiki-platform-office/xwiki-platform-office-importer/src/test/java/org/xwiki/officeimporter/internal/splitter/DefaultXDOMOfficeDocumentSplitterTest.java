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
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;
import org.xwiki.officeimporter.internal.AbstractOfficeImporterTest;
import org.xwiki.officeimporter.splitter.OfficeDocumentSplitterParameters;
import org.xwiki.officeimporter.splitter.TargetDocumentDescriptor;
import org.xwiki.officeimporter.splitter.XDOMOfficeDocumentSplitter;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultXDOMOfficeDocumentSplitter}.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@ComponentTest
class DefaultXDOMOfficeDocumentSplitterTest extends AbstractOfficeImporterTest
{
    /**
     * Parser for building XDOM instances.
     */
    private Parser xwikiSyntaxParser;

    /**
     * Document splitter for testing.
     */
    private XDOMOfficeDocumentSplitter officeDocumentSplitter;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwikiSyntaxParser = this.componentManager.getInstance(Parser.class, Syntax.XWIKI_2_1.toIdString());
        this.officeDocumentSplitter = this.componentManager.getInstance(XDOMOfficeDocumentSplitter.class);
    }

    /**
     * Test basic document splitting.
     * 
     * @throws Exception if it fails to parse the wiki syntax or if it fails to split the document
     */
    @Test
    void documentSplitting() throws Exception
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
            new XDOMOfficeDocument(xdom, Collections.emptyMap(), this.componentManager, null);
        final DocumentReference baseDocument = new DocumentReference("xwiki", "Test", "Test");

        // Add expectations to mock document name serializer.
        when(this.mockCompactWikiStringEntityReferenceSerializer.serialize(baseDocument)).thenReturn("Test.Test");
        when(this.mockCompactWikiStringEntityReferenceSerializer
            .serialize(new DocumentReference("xwiki", "Test", "Heading1"))).thenReturn("Test.Heading1");
        when(this.mockCompactWikiStringEntityReferenceSerializer
            .serialize(new DocumentReference("xwiki", "Test", "Heading11"))).thenReturn("Test.Heading11");
        when(this.mockCompactWikiStringEntityReferenceSerializer
            .serialize(new DocumentReference("xwiki", "Test", "Heading12"))).thenReturn("Test.Heading12");
        when(this.mockCompactWikiStringEntityReferenceSerializer
            .serialize(new DocumentReference("xwiki", "Test", "Heading2"))).thenReturn("Test.Heading2");

        // Add expectations to mock document name factory.
        when(this.mockDocumentReferenceResolver.resolve("Test.Test"))
            .thenReturn(new DocumentReference("xwiki", "Test", "Test"));
        when(this.mockDocumentReferenceResolver.resolve("xwiki:Test.Heading1"))
            .thenReturn(new DocumentReference("xwiki", "Test", "Heading1"));
        when(this.mockDocumentReferenceResolver.resolve("xwiki:Test.Heading11"))
            .thenReturn(new DocumentReference("xwiki", "Test", "Heading11"));
        when(this.mockDocumentReferenceResolver.resolve("xwiki:Test.Heading12"))
            .thenReturn(new DocumentReference("xwiki", "Test", "Heading12"));
        when(this.mockDocumentReferenceResolver.resolve("xwiki:Test.Heading2"))
            .thenReturn(new DocumentReference("xwiki", "Test", "Heading2"));

        // Perform the split operation.
        OfficeDocumentSplitterParameters parameters = new OfficeDocumentSplitterParameters();
        parameters.setHeadingLevelsToSplit(new int[] {1, 2, 3, 4, 5, 6});
        parameters.setNamingCriterionHint("headingNames");
        parameters.setBaseDocumentReference(baseDocument);
        Map<TargetDocumentDescriptor, XDOMOfficeDocument> result =
            this.officeDocumentSplitter.split(officeDocument, parameters);

        // There should be five XDOM office documents.
        assertEquals(5, result.size());
    }
}
