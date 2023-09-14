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
package org.xwiki.export.pdf.internal.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;

/**
 * Unit tests for {@link DocumentRendererParameters}.
 */
class DocumentRendererParametersTest
{
    private DocumentRendererParameters parameters = new DocumentRendererParameters();

    @Test
    void verifyToString()
    {
        parameters = parameters.withTitle(true).withMetadataReference(
            new ObjectPropertyReference("metadata", new ObjectReference("XWiki.PDFExport.TemplateClass[0]",
                new DocumentReference("test", Arrays.asList("XWiki", "PDFExport"), "Template"))));
        assertEquals(
            "withTitle = [true], metadataReference = "
                + "[Object_property test:XWiki.PDFExport.Template^XWiki.PDFExport.TemplateClass[0].metadata]",
            parameters.toString());
    }

    @Test
    void verifyEquals()
    {
        assertNotEquals(null, parameters);
        assertNotEquals("test", parameters);
        assertEquals(parameters, parameters);
        assertEquals(parameters, new DocumentRendererParameters());
        assertNotEquals(parameters, new DocumentRendererParameters().withTitle(true));
    }

    @Test
    void verifyHashCode()
    {
        assertEquals(new DocumentRendererParameters().hashCode(), parameters.hashCode());
        assertNotEquals(new DocumentRendererParameters().withTitle(true).hashCode(), parameters.hashCode());
    }
}
