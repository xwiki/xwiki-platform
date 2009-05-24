package org.xwiki.rendering.example;

import java.io.StringReader;

import junit.framework.TestCase;

import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationManager;

public class ExampleTest extends TestCase
{
    public void test() throws Exception
    {
        // Initialize Rendering components and allow getting instances
        EmbeddableComponentManager ecm = new EmbeddableComponentManager(this.getClass().getClassLoader());
        ecm.initialize();
        
        // Parse XWiki 2.0 Syntax
        Parser parser = (Parser) ecm.lookup(Parser.class, Syntax.XWIKI_2_0.toIdString());
        XDOM xdom = parser.parse(new StringReader("This is **bold**"));
        
        // Run macros
        TransformationManager txManager = (TransformationManager) ecm.lookup(TransformationManager.class);
        txManager.performTransformations(xdom, parser.getSyntax());

        // Generate HTML for example
        WikiPrinter printer = new DefaultWikiPrinter();
        PrintRendererFactory rf = (PrintRendererFactory) ecm.lookup(PrintRendererFactory.class);
        Renderer htmlRenderer = rf.createRenderer(Syntax.XHTML_1_0, printer);
        
        xdom.traverse(htmlRenderer);

        assertEquals("<p>This is <strong>bold</strong></p>", printer.toString());
    }
}
