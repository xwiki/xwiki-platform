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

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xwiki.annotation.TestDocumentFactory;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.renderer.DefaultLinkLabelGenerator;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.test.MockWikiModel;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Renderer tests for the XHTML annotations renderer, from the test files.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@RunWith(Parameterized.class)
public class AnnotationXHTMLRendererTest extends AbstractComponentTestCase
{
    /**
     * Document description files to run this test for.
     */
    private static Collection<String[]> files = new ArrayList<String[]>();

    /**
     * The annotations renderer hint.
     */
    private static final String ANNOTATIONS_RENDERER_HINT = "annotations-xhtml/1.0";

    /**
     * Mock document to run tests for.
     */
    protected String docName;

    /**
     * Factory to load test documents.
     */
    protected TestDocumentFactory docFactory;

    static {
        // tests containing only plain text content
        addFileToTest("renderer/plain/Plain1.test");
        addFileToTest("renderer/plain/Plain2.test");
        addFileToTest("renderer/plain/Plain3.test");
        addFileToTest("renderer/plain/Plain4.test");
        addFileToTest("renderer/plain/Plain5.test");
        addFileToTest("renderer/plain/Plain6.test");

        // tests containing formatting
        addFileToTest("renderer/format/Format1.test");
        addFileToTest("renderer/format/Format2.test");
        addFileToTest("renderer/format/Format3.test");
        addFileToTest("renderer/format/Format4.test");
        addFileToTest("renderer/format/Format5.test");

        // tests containing special characters in the annotated content
        addFileToTest("renderer/specialchars/SpecialChars1.test");
        addFileToTest("renderer/specialchars/SpecialChars2.test");
        addFileToTest("renderer/specialchars/SpecialChars3.test");

        // tests for which the selection of the annotation appears more than once in the document content
        addFileToTest("renderer/ambiguous/Ambiguous1.test");
        addFileToTest("renderer/ambiguous/Ambiguous2.test");
        addFileToTest("renderer/ambiguous/Ambiguous3.test");
        addFileToTest("renderer/ambiguous/Ambiguous4.test");
        addFileToTest("renderer/ambiguous/Ambiguous5.test");
        addFileToTest("renderer/ambiguous/Ambiguous6.test");
        // FIXME: fix support for empty selection annotations by making sure that, at each point, for the same
        // annotation, startEvents & end events are sent in this order. FTM the convention is that annotations are
        // closed before are opened, for which reason the annotation is something like: </span><span
        // class="annotation annotationID0">...
        // addFileToTest("renderer/ambiguous/Ambiguous7.test");

        // tests in which more than one annotation needs to be rendered in the content
        addFileToTest("renderer/multiple/Multiple1.test");
        addFileToTest("renderer/multiple/Multiple2.test");
        addFileToTest("renderer/multiple/Multiple3.test");
        addFileToTest("renderer/multiple/Multiple4.test");
        addFileToTest("renderer/multiple/Multiple5.test");

        // tests containing links in the annotated content
        addFileToTest("renderer/links/Links1.test");
        addFileToTest("renderer/links/Links2.test");
        addFileToTest("renderer/links/Links3.test");
        addFileToTest("renderer/links/Links4.test");
        addFileToTest("renderer/links/Links5.test");
        addFileToTest("renderer/links/Links6.test");
        addFileToTest("renderer/links/Links7.test");
        addFileToTest("renderer/links/Links8.test");

        // tests containing macros generating content in the annotated content
        addFileToTest("renderer/macros/Macros1.test");
        addFileToTest("renderer/macros/Macros2.test");
        addFileToTest("renderer/macros/Macros3.test");
        addFileToTest("renderer/macros/Macros4.test");
        addFileToTest("renderer/macros/Macros5.test");
        addFileToTest("renderer/macros/Macros6.test");
        addFileToTest("renderer/macros/Macros7.test");

        // tests where the annotated content is in a table
        addFileToTest("renderer/tables/Tables1.test");
        addFileToTest("renderer/tables/Tables2.test");

        // tests where the annotated content is inside some verbatim blocks
        addFileToTest("renderer/verbatim/Verbatim1.test");
        addFileToTest("renderer/verbatim/Verbatim2.test");
        addFileToTest("renderer/verbatim/Verbatim3.test");
        addFileToTest("renderer/verbatim/Verbatim4.test");
        addFileToTest("renderer/verbatim/Verbatim5.test");
        addFileToTest("renderer/verbatim/Verbatim6.test");
        addFileToTest("renderer/verbatim/Verbatim7.test");
        addFileToTest("renderer/verbatim/Verbatim8.test");
        addFileToTest("renderer/verbatim/Verbatim9.test");
        addFileToTest("renderer/verbatim/Verbatim10.test");
        addFileToTest("renderer/verbatim/Verbatim11.test");

        // tests where annotations start and/or end in the middle of a word rather than at the beginning or end
        addFileToTest("renderer/partialwords/PartialWords1.test");
        addFileToTest("renderer/partialwords/PartialWords2.test");
        addFileToTest("renderer/partialwords/PartialWords3.test");
        addFileToTest("renderer/partialwords/PartialWords4.test");
        addFileToTest("renderer/partialwords/PartialWords5.test");
        addFileToTest("renderer/partialwords/PartialWords6.test");

        addFileToTest("renderer/spaces/Spaces1.test");
        addFileToTest("renderer/spaces/Spaces2.test");
        addFileToTest("renderer/spaces/Spaces3.test");
    }

    /**
     * Creates a test for the passed document. Will be instantiated by the parameterized runner for all the parameters.
     * 
     * @param docName the document (and corpus filename) to run tests for
     */
    public AnnotationXHTMLRendererTest(String docName)
    {
        this.docName = docName;
    }

    /**
     * Adds a file to the list of files to run tests for.
     * 
     * @param docName the name of the document / file to test
     */
    private static void addFileToTest(String docName)
    {
        files.add(new String[] {docName});
    }

    /**
     * @return list of corpus files to instantiate tests for
     */
    @Parameters
    public static Collection<String[]> data()
    {
        return files;
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // register wiki model mock so that we can use documents / attachments information
        getComponentManager().registerComponent(MockWikiModel.getComponentDescriptor());
        // make sure to use the default link label generator
        registerComponent(DefaultLinkLabelGenerator.class);
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        docFactory = new TestDocumentFactory();
    }

    /**
     * Test rendering the annotations in the document description file results in the annotated html.
     * 
     * @throws Exception in case something goes wrong looking up components and rendering
     */
    @Test
    public void getAnnotatedHTML() throws Exception
    {
        Parser parser = getComponentManager().getInstance(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));
        SyntaxFactory syntaxFactory = getComponentManager().getInstance(SyntaxFactory.class);

        // run transformations
        TransformationManager transformationManager = getComponentManager().getInstance(TransformationManager.class);
        TransformationContext context = new TransformationContext(xdom,
            syntaxFactory.createSyntaxFromIdString(docFactory.getDocument(docName).getSyntax()));
        context.setTargetSyntax(Syntax.ANNOTATED_XHTML_1_0);
        transformationManager.performTransformations(xdom, context);

        AnnotationPrintRenderer renderer =
            getComponentManager().getInstance(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);
        // set the annotations for this renderer
        renderer.setAnnotations(docFactory.getDocument(docName).getAnnotations());

        xdom.traverse(renderer);

        assertEquals("[" + docName + "] test failed", docFactory.getDocument(docName).getAnnotatedContent(), printer
            .toString());
    }

    /**
     * Test rendering with the annotations renderer but without annotations doesn't alter the content.
     * 
     * @throws Exception in case something goes wrong looking up components and rendering
     */
    @Test
    public void getAnnotatedHTMLWithoutAnnotations() throws Exception
    {
        Parser parser = getComponentManager().getInstance(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));
        SyntaxFactory syntaxFactory = getComponentManager().getInstance(SyntaxFactory.class);

        // run transformations
        TransformationManager transformationManager = getComponentManager().getInstance(TransformationManager.class);
        TransformationContext context = new TransformationContext(xdom,
            syntaxFactory.createSyntaxFromIdString(docFactory.getDocument(docName).getSyntax()));
        context.setTargetSyntax(Syntax.ANNOTATED_XHTML_1_0);
        transformationManager.performTransformations(xdom, context);

        AnnotationPrintRenderer renderer =
            getComponentManager().getInstance(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        xdom.traverse(renderer);

        assertEquals("[" + docName + "] test failed", docFactory.getDocument(docName).getRenderedContent(), printer
            .toString());
    }
}
