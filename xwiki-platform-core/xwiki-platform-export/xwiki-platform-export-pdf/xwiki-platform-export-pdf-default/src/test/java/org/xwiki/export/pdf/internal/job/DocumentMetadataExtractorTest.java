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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Unit tests for {@link DocumentMetadataExtractor}.
 * 
 * @version $Id$
 */
@ComponentTest
class DocumentMetadataExtractorTest
{
    @InjectMockComponents
    private DocumentMetadataExtractor extractor;

    /**
     * Capture logs.
     */
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Execution execution;

    private ExecutionContext executionContext = new ExecutionContext();

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument templateDocument;

    private DocumentReference templateReference = new DocumentReference("test", "Some", "PDFTemplate");

    private ObjectPropertyReference metadataReference = new ObjectPropertyReference("metadata",
        new ObjectReference("XWiki.PDFExport.TemplateClass[0]", templateReference));

    @Mock
    private BaseObject templateObject;

    @Mock
    private XWikiDocument sourceDocument;

    private DocumentReference sourceDocumentReference = new DocumentReference("test", "Some", "Page");

    @BeforeEach
    void configure() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(this.metadataReference.extractReference(EntityType.DOCUMENT), this.xcontext))
            .thenReturn(templateDocument);

        when(this.sourceDocument.getDocumentReference()).thenReturn(sourceDocumentReference);
    }

    @Test
    void getWithoutMetadataReference()
    {
        Map<String, String> metadata = this.extractor.getMetadata(this.sourceDocument, null);

        assertTrue(metadata.isEmpty());
    }

    @Test
    void getWithoutExecutionContext()
    {
        Map<String, String> metadata = this.extractor.getMetadata(this.sourceDocument, this.metadataReference);

        assertTrue(metadata.isEmpty());
    }

    @Test
    void getWithException()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.sourceDocument.display(this.metadataReference.getName(), (BaseObject) null, this.xcontext))
            .thenThrow(new RuntimeException("Some error message"));

        Map<String, String> metadata = this.extractor.getMetadata(this.sourceDocument, this.metadataReference);

        assertTrue(metadata.isEmpty());
        assertEquals(String.format("Failed to get the metadata for document [%s] from [%s]. Root cause is [%s].",
            this.sourceDocument.getDocumentReference(), this.metadataReference, "RuntimeException: Some error message"),
            logCapture.getMessage(0));
    }

    @Test
    void getMetadata()
    {
        when(this.execution.getContext()).thenReturn(this.executionContext);
        when(this.templateDocument.getXObject(this.metadataReference.getParent())).thenReturn(this.templateObject);
        when(this.sourceDocument.display(this.metadataReference.getName(), this.templateObject, this.xcontext)).thenAnswer(
            invocation -> {
                Map<String, String> metadata = (Map<String, String>) this.executionContext.getProperty(
                    DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA);
                metadata.put("data-foo", "bar");
                return null;
            });

        Map<String, String> metadata = this.extractor.getMetadata(this.sourceDocument, this.metadataReference);

        // Verify the returned metadata.
        assertEquals(1, metadata.size());
        assertEquals("bar", metadata.get("data-foo"));

        // Verify that the execution context was properly cleaned up.
        assertFalse(this.executionContext.hasProperty(DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA));
    }
}
