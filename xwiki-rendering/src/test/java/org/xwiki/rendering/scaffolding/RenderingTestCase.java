package org.xwiki.rendering.scaffolding;

import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.renderer.PrintRenderer;

import java.io.StringReader;

public class RenderingTestCase extends AbstractRenderingTestCase
{
    private String input;
    private String expected;
    private Parser parser;
    private PrintRenderer renderer;
    private boolean runTransformations;

    public RenderingTestCase(String testName, String input, String expected, Parser parser,
        PrintRenderer renderer, boolean runTransformations)
    {
        super(testName);
        this.input = input;
        this.expected = expected;
        this.parser = parser;
        this.renderer = renderer;
        this.runTransformations = runTransformations;
    }

    @Override
    protected void runTest() throws Throwable
    {
        XDOM dom = this.parser.parse(new StringReader(this.input));

        if (this.runTransformations) {
            TransformationManager transformationManager =
                (TransformationManager) getComponentManager().lookup(TransformationManager.ROLE);
            transformationManager.performTransformations(dom, this.parser.getSyntax());
        }

        dom.traverse(this.renderer);

        assertEquals(this.expected, this.renderer.getPrinter().toString());
    }
}
