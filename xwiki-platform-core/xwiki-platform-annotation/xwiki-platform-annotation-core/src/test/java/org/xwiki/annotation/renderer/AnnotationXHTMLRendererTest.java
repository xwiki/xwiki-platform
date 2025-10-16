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
package org.xwiki.annotation.renderer;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.Mock;
import org.xwiki.annotation.TestDocumentFactory;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Renderer tests for the XHTML annotations renderer, from the test files.
 *
 * @version $Id$
 * @since 2.3M1
 */
@AllComponents
@OldcoreTest
class AnnotationXHTMLRendererTest
{
    /**
     * Document description files to run this test for.
     */
    private static final List<String> files = List.of(
        // tests containing only plain text content
        "renderer/plain/Plain1.test",
        "renderer/plain/Plain2.test",
        "renderer/plain/Plain3.test",
        "renderer/plain/Plain4.test",
        "renderer/plain/Plain5.test",
        "renderer/plain/Plain6.test",

        // tests containing formatting
        "renderer/format/Format1.test",
        "renderer/format/Format2.test",
        "renderer/format/Format3.test",
        "renderer/format/Format4.test",
        "renderer/format/Format5.test",

        // tests containing special characters in the annotated content
        "renderer/specialchars/SpecialChars1.test",
        "renderer/specialchars/SpecialChars2.test",
        "renderer/specialchars/SpecialChars3.test",
        "renderer/specialchars/SpecialChars4.test",
        "renderer/specialchars/SpecialChars5.test",

        // tests for which the selection of the annotation appears more than once in the document content
        "renderer/ambiguous/Ambiguous1.test",
        "renderer/ambiguous/Ambiguous2.test",
        "renderer/ambiguous/Ambiguous3.test",
        "renderer/ambiguous/Ambiguous4.test",
        "renderer/ambiguous/Ambiguous5.test",
        "renderer/ambiguous/Ambiguous6.test",
        // FIXME: fix support for empty selection annotations by making sure that, at each point, for the same
        // annotation, startEvents & end events are sent in this order. FTM the convention is that annotations are
        // closed before are opened, for which reason the annotation is something like: </span><span
        // class="annotation annotationID0">...
        // "renderer/ambiguous/Ambiguous7.test",

        // tests in which more than one annotation needs to be rendered in the content
        "renderer/multiple/Multiple1.test",
        "renderer/multiple/Multiple2.test",
        "renderer/multiple/Multiple3.test",
        "renderer/multiple/Multiple4.test",
        "renderer/multiple/Multiple5.test",

        // tests containing links in the annotated content
        "renderer/links/Links1.test",
        "renderer/links/Links2.test",
        "renderer/links/Links3.test",
        "renderer/links/Links4.test",
        "renderer/links/Links5.test",
        "renderer/links/Links6.test",
        "renderer/links/Links7.test",
        "renderer/links/Links8.test",

        // tests containing macros generating content in the annotated content
        "renderer/macros/Macros1.test",
        "renderer/macros/Macros2.test",
        "renderer/macros/Macros3.test",
        "renderer/macros/Macros4.test",
        "renderer/macros/Macros5.test",
        "renderer/macros/Macros6.test",
        "renderer/macros/Macros7.test",

        // tests where the annotated content is in a table
        "renderer/tables/Tables1.test",
        "renderer/tables/Tables2.test",

        // tests where the annotated content is inside some verbatim blocks
        "renderer/verbatim/Verbatim1.test",
        "renderer/verbatim/Verbatim2.test",
        "renderer/verbatim/Verbatim3.test",
        "renderer/verbatim/Verbatim4.test",
        "renderer/verbatim/Verbatim5.test",
        "renderer/verbatim/Verbatim6.test",
        "renderer/verbatim/Verbatim7.test",
        "renderer/verbatim/Verbatim8.test",
        "renderer/verbatim/Verbatim9.test",
        "renderer/verbatim/Verbatim10.test",
        "renderer/verbatim/Verbatim11.test",

        // tests where annotations start and/or end in the middle of a word rather than at the beginning or end
        "renderer/partialwords/PartialWords1.test",
        "renderer/partialwords/PartialWords2.test",
        "renderer/partialwords/PartialWords3.test",
        "renderer/partialwords/PartialWords4.test",
        "renderer/partialwords/PartialWords5.test",
        "renderer/partialwords/PartialWords6.test",

        "renderer/spaces/Spaces1.test",
        "renderer/spaces/Spaces2.test",
        "renderer/spaces/Spaces3.test"
    );

    @InjectComponentManager
    private ComponentManager componentManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Mock
    private XWikiURLFactory urlFactory;

    /**
     * The annotations renderer hint.
     */
    private static final String ANNOTATIONS_RENDERER_HINT = "annotations-xhtml/1.0";

    private TestDocumentFactory docFactory;

    @BeforeEach
    void setUp() throws Exception
    {
        docFactory = new TestDocumentFactory();

        XWikiContext context = this.oldcore.getXWikiContext();
        context.setURLFactory(this.urlFactory);
        when(this.urlFactory.getURL(any(), eq(context))).thenReturn("viewurl");
        this.oldcore.getSpyXWiki().saveDocument(
            new XWikiDocument(new DocumentReference("xwiki", "Space", "ExistingPage")), context);
    }

    /**
     * Test rendering the annotations in the document description file results in the annotated html.
     *
     * @throws Exception in case something goes wrong looking up components and rendering
     */
    @ParameterizedTest
    @FieldSource("files")
    void getAnnotatedHTML(String docName) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));

        // run transformations
        TransformationManager transformationManager = this.componentManager.getInstance(TransformationManager.class);
        TransformationContext context = new TransformationContext(xdom,
            Syntax.valueOf(docFactory.getDocument(docName).getSyntax()));
        context.setTargetSyntax(Syntax.ANNOTATED_XHTML_1_0);
        transformationManager.performTransformations(xdom, context);

        AnnotationPrintRenderer renderer =
            this.componentManager.getInstance(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);
        // set the annotations for this renderer
        renderer.setAnnotations(docFactory.getDocument(docName).getAnnotations());

        xdom.traverse(renderer);

        assertEquals(docFactory.getDocument(docName).getAnnotatedContent(),
            printer.toString(), "[" + docName + "] test failed");
    }

    /**
     * Test rendering with the annotations renderer but without annotations doesn't alter the content.
     *
     * @throws Exception in case something goes wrong looking up components and rendering
     */
    @ParameterizedTest
    @FieldSource("files")
    void getAnnotatedHTMLWithoutAnnotations(String docName) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));

        // run transformations
        TransformationManager transformationManager = this.componentManager.getInstance(TransformationManager.class);
        TransformationContext context = new TransformationContext(xdom,
            Syntax.valueOf(docFactory.getDocument(docName).getSyntax()));
        context.setTargetSyntax(Syntax.ANNOTATED_XHTML_1_0);
        transformationManager.performTransformations(xdom, context);

        AnnotationPrintRenderer renderer =
            this.componentManager.getInstance(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        xdom.traverse(renderer);

        assertEquals(docFactory.getDocument(docName).getRenderedContent(),
            printer.toString(), "[" + docName + "] test failed");
    }
}
