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
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.scaffolding.MockWikiModel;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.test.AbstractComponentTestCase;

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
        addFileToTest("renderer/plain/Plain1");
        addFileToTest("renderer/plain/Plain2");
        addFileToTest("renderer/plain/Plain3");
        addFileToTest("renderer/plain/Plain4");
        addFileToTest("renderer/plain/Plain5");
        addFileToTest("renderer/plain/Plain6");

        // tests containing formatting
        addFileToTest("renderer/format/Format1");
        addFileToTest("renderer/format/Format2");
        addFileToTest("renderer/format/Format3");
        addFileToTest("renderer/format/Format4");
        addFileToTest("renderer/format/Format5");

        // tests containing special characters in the annotated content
        addFileToTest("renderer/specialchars/SpecialChars1");
        addFileToTest("renderer/specialchars/SpecialChars2");
        addFileToTest("renderer/specialchars/SpecialChars3");

        // tests for which the selection of the annotation appears more than once in the document content
        addFileToTest("renderer/ambiguous/Ambiguous1");
        addFileToTest("renderer/ambiguous/Ambiguous2");
        addFileToTest("renderer/ambiguous/Ambiguous3");
        addFileToTest("renderer/ambiguous/Ambiguous4");
        addFileToTest("renderer/ambiguous/Ambiguous5");
        addFileToTest("renderer/ambiguous/Ambiguous6");
        // FIXME: fix support for empty selection annotations by making sure that, at each point, for the same
        // annotation, startEvents & end events are sent in this order. FTM the convention is that annotations are
        // closed before are opened, for which reason the annotation is something like: </span><span
        // class="annotation annotationID0">...
        // addFileToTest("renderer/ambiguous/Ambiguous7");

        // tests in which more than one annotation needs to be rendered in the content
        addFileToTest("renderer/multiple/Multiple1");
        addFileToTest("renderer/multiple/Multiple2");
        addFileToTest("renderer/multiple/Multiple3");
        addFileToTest("renderer/multiple/Multiple4");
        addFileToTest("renderer/multiple/Multiple5");

        // tests containing links in the annotated content
        addFileToTest("renderer/links/Links1");
        addFileToTest("renderer/links/Links2");
        addFileToTest("renderer/links/Links3");
        addFileToTest("renderer/links/Links4");
        addFileToTest("renderer/links/Links5");
        addFileToTest("renderer/links/Links6");
        addFileToTest("renderer/links/Links7");
        addFileToTest("renderer/links/Links8");

        // tests containing macros generating content in the annotated content
        addFileToTest("renderer/macros/Macros1");
        addFileToTest("renderer/macros/Macros2");
        addFileToTest("renderer/macros/Macros3");
        addFileToTest("renderer/macros/Macros4");
        addFileToTest("renderer/macros/Macros5");
        addFileToTest("renderer/macros/Macros6");
        addFileToTest("renderer/macros/Macros7");

        // tests where the annotated content is in a table
        addFileToTest("renderer/tables/Tables1");
        addFileToTest("renderer/tables/Tables2");

        // tests where the annotated content is inside some verbatim blocks
        addFileToTest("renderer/verbatim/Verbatim1");
        addFileToTest("renderer/verbatim/Verbatim2");
        addFileToTest("renderer/verbatim/Verbatim3");
        addFileToTest("renderer/verbatim/Verbatim4");
        addFileToTest("renderer/verbatim/Verbatim5");
        addFileToTest("renderer/verbatim/Verbatim6");
        addFileToTest("renderer/verbatim/Verbatim7");
        addFileToTest("renderer/verbatim/Verbatim8");
        addFileToTest("renderer/verbatim/Verbatim9");
        addFileToTest("renderer/verbatim/Verbatim10");

        // tests where annotations start and/or end in the middle of a word rather than at the beginning or end
        addFileToTest("renderer/partialwords/PartialWords1");
        addFileToTest("renderer/partialwords/PartialWords2");
        addFileToTest("renderer/partialwords/PartialWords3");
        addFileToTest("renderer/partialwords/PartialWords4");
        addFileToTest("renderer/partialwords/PartialWords5");
        addFileToTest("renderer/partialwords/PartialWords6");

        addFileToTest("renderer/spaces/Spaces1");
        addFileToTest("renderer/spaces/Spaces2");
        addFileToTest("renderer/spaces/Spaces3");
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractComponentTestCase#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        // register wiki model mock so that we can use documents / attachments information
        getComponentManager().registerComponent(MockWikiModel.getComponentDescriptor());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractComponentTestCase#setUp()
     */
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
        Parser parser = getComponentManager().lookup(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));
        SyntaxFactory syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);

        // run transformations
        TransformationManager transformationManager = getComponentManager().lookup(TransformationManager.class);
        transformationManager.performTransformations(xdom, syntaxFactory.createSyntaxFromIdString(docFactory
            .getDocument(docName).getSyntax()));

        AnnotationPrintRenderer renderer =
            getComponentManager().lookup(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
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
        Parser parser = getComponentManager().lookup(Parser.class, docFactory.getDocument(docName).getSyntax());
        XDOM xdom = parser.parse(new StringReader(docFactory.getDocument(docName).getSource()));
        SyntaxFactory syntaxFactory = getComponentManager().lookup(SyntaxFactory.class);

        // run transformations
        TransformationManager transformationManager = getComponentManager().lookup(TransformationManager.class);
        transformationManager.performTransformations(xdom, syntaxFactory.createSyntaxFromIdString(docFactory
            .getDocument(docName).getSyntax()));

        AnnotationPrintRenderer renderer =
            getComponentManager().lookup(AnnotationPrintRenderer.class, ANNOTATIONS_RENDERER_HINT);
        WikiPrinter printer = new DefaultWikiPrinter();
        renderer.setPrinter(printer);

        xdom.traverse(renderer);

        assertEquals("[" + docName + "] test failed", docFactory.getDocument(docName).getRenderedContent(), printer
            .toString());
    }
}
