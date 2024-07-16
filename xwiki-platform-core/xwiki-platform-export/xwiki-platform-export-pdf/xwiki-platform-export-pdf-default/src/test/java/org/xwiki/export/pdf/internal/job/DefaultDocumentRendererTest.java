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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link DefaultDocumentRenderer}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultDocumentRendererTest
{
    @InjectMockComponents
    private DefaultDocumentRenderer documentRenderer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private DocumentMetadataExtractor metadataExtractor;

    @Mock
    private BlockRenderer html5Renderer;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki wiki;

    private DocumentReference documentReference = new DocumentReference("test", "Some", "Page");

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument translatedDocument;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(this.documentReference, this.xcontext)).thenReturn(document);
        when(this.document.getTranslatedDocument(this.xcontext)).thenReturn(this.translatedDocument);
        when(this.document.getDocumentReference()).thenReturn(this.documentReference);
        when(this.translatedDocument.getDocumentReference()).thenReturn(this.documentReference);
        when(this.entityReferenceSerializer.serialize(this.documentReference)).thenReturn("test:Some.Page");

        when(this.renderingContext.getTargetSyntax()).thenReturn(Syntax.HTML_5_0);
        when(this.contextComponentManager.getInstance(BlockRenderer.class, Syntax.HTML_5_0.toIdString()))
            .thenReturn(this.html5Renderer);
    }

    @Test
    void render() throws Exception
    {
        XDOM titleXDOM = new XDOM(Arrays.asList(new WordBlock("title")));
        XDOM xdom = new XDOM(Arrays.asList(new WordBlock("content")));
        when(this.documentDisplayer.display(same(this.translatedDocument), any(DocumentDisplayerParameters.class)))
            .then(new Answer<XDOM>()
            {
                @Override
                public XDOM answer(InvocationOnMock invocation) throws Throwable
                {
                    DocumentDisplayerParameters params = (DocumentDisplayerParameters) invocation.getArgument(1);
                    params.getIdGenerator().generateUniqueId("H", "heading");
                    return params.isTitleDisplayed() ? titleXDOM : xdom;
                }
            });

        doAnswer(new Answer<Void>()
        {
            public Void answer(InvocationOnMock invocation)
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("some content");
                return null;
            }
        }).when(this.html5Renderer).render(same(xdom), any(WikiPrinter.class));

        DocumentRendererParameters rendererParameters = new DocumentRendererParameters();
        DocumentRenderingResult result = this.documentRenderer.render(documentReference, rendererParameters);
        assertEquals(documentReference, result.getDocumentReference());
        assertEquals(1, xdom.getChildren().size());
        assertSame(xdom, result.getXDOM());
        assertEquals("some content", result.getHTML());
        assertEquals(Collections.singletonMap("Hheading", "Hheading"), result.getIdMap());

        // Now render with title and metadata.
        ObjectPropertyReference metadataReference =
            new ObjectPropertyReference("metadata", new ObjectReference("XWiki.PDFExport.TemplateClass[0]",
                new DocumentReference("test", "Some", "PDFTemplate")));
        rendererParameters = rendererParameters.withTitle(true).withMetadataReference(metadataReference);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("data-foo", "bar");
        metadata.put("data-xwiki-rendering-protected", "true");
        when(this.metadataExtractor.getMetadata(translatedDocument, metadataReference)).thenReturn(metadata);
        result = this.documentRenderer.render(documentReference, rendererParameters);
        assertEquals(2, xdom.getChildren().size());
        HeaderBlock title = (HeaderBlock) xdom.getChildren().get(0);
        assertEquals(HeaderLevel.LEVEL1, title.getLevel());
        assertEquals("Htest:Some.Page", title.getId());
        assertEquals("test:Some.Page", title.getParameter("data-xwiki-document-reference"));
        assertEquals("bar", title.getParameter("data-foo"));
        assertEquals("true", title.getParameter("data-xwiki-rendering-protected"));
        assertEquals("title", ((WordBlock) title.getChildren().get(0)).getWord());

        Map<String, String> expectedIdMap = new HashMap<>();
        expectedIdMap.put("Hheading", "Hheading-1");
        expectedIdMap.put("Hheading-1", "Hheading-2");
        assertEquals(expectedIdMap, result.getIdMap());
    }

    @Test
    void renderCurrentDocument() throws Exception
    {
        when(this.xcontext.getDoc()).thenReturn(this.document);
        when(this.document.getTranslatedDocument(this.xcontext)).thenReturn(this.document);

        DocumentRendererParameters rendererParameters = new DocumentRendererParameters();
        this.documentRenderer.render(this.documentReference, rendererParameters);

        verify(this.documentDisplayer).display(same(this.document), any(DocumentDisplayerParameters.class));
    }
}
