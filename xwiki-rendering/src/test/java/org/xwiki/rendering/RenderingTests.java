package org.xwiki.rendering;

import junit.framework.Test;
import junit.framework.TestCase;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.rendering.scaffolding.TestEventsRenderer;
import org.xwiki.rendering.scaffolding.PrintRendererFactory;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.TexRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLRenderer;
import org.xwiki.rendering.renderer.xhtml.WysiwygEditorXHTMLRenderer;
import org.xwiki.rendering.MockDocumentManager;

import java.util.Map;
import java.util.HashMap;

public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        Map<String, PrintRendererFactory> factories = new HashMap<String, PrintRendererFactory>();
        factories.put("xwiki", new PrintRendererFactory() {
            public PrintRenderer createRenderer()
            {
                return new XWikiSyntaxRenderer(new DefaultWikiPrinter());
            }
        });
        factories.put("event", new PrintRendererFactory() {
            public PrintRenderer createRenderer()
            {
                return new TestEventsRenderer(new DefaultWikiPrinter());
            }
        });
        factories.put("xhtml", new PrintRendererFactory() {
            public PrintRenderer createRenderer()
            {
                return new XHTMLRenderer(new DefaultWikiPrinter(), new MockDocumentManager());
            }
        });
        factories.put("wysiwyg", new PrintRendererFactory() {
            public PrintRenderer createRenderer()
            {
                return new WysiwygEditorXHTMLRenderer(new DefaultWikiPrinter(), new MockDocumentManager());
            }
        });
        factories.put("tex", new PrintRendererFactory() {
            public PrintRenderer createRenderer()
            {
                return new TexRenderer(new DefaultWikiPrinter());
            }
        });

        RenderingTestSuite suite = new RenderingTestSuite("Test all Parsers/Renderers", factories);

        suite.addTestsFromResource("bold/bold1", false);
        suite.addTestsFromResource("bold/bold2", false);
        suite.addTestsFromResource("bold/bold3", false);
        suite.addTestsFromResource("bold/bold4", false);
        suite.addTestsFromResource("bold/bold5", false);
        suite.addTestsFromResource("bold/bold6", false);
        suite.addTestsFromResource("bold/bold7", false);
        suite.addTestsFromResource("escape", false);
        suite.addTestsFromResource("horizontalline", false);
        suite.addTestsFromResource("html", false);
        suite.addTestsFromResource("links", false);
        suite.addTestsFromResource("list", false);
        suite.addTestsFromResource("macros/macro", false);
        suite.addTestsFromResource("macros/macrohtml", true);
        suite.addTestsFromResource("macros/macronowiki", true);
        suite.addTestsFromResource("macros/macrovelocity", true);
        suite.addTestsFromResource("macros/macroxhtml", true);
        suite.addTestsFromResource("paragraph", false);
        suite.addTestsFromResource("section", false);

        return suite;
    }
}
