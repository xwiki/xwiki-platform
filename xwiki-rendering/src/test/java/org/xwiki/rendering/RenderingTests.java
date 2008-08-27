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
package org.xwiki.rendering;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;

import org.xwiki.rendering.renderer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.TexRenderer;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
import org.xwiki.rendering.renderer.xhtml.WysiwygEditorXHTMLRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLRenderer;
import org.xwiki.rendering.scaffolding.PrintRendererFactory;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;

import com.xpn.xwiki.test.PlexusTestSetup;

/**
 * All Rendering integration tests.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        Map<String, PrintRendererFactory> factories = new HashMap<String, PrintRendererFactory>();
        factories.put("xwiki", new PrintRendererFactory()
        {
            public PrintRenderer createRenderer()
            {
                return new XWikiSyntaxRenderer(new DefaultWikiPrinter());
            }
        });
        factories.put("event", new PrintRendererFactory()
        {
            public PrintRenderer createRenderer()
            {
                return new EventsRenderer(new DefaultWikiPrinter());
            }
        });
        factories.put("xhtml", new PrintRendererFactory()
        {
            public PrintRenderer createRenderer()
            {
                return new XHTMLRenderer(new DefaultWikiPrinter(), new MockDocumentAccessBridge(), null);
            }
        });
        factories.put("wysiwyg", new PrintRendererFactory()
        {
            public PrintRenderer createRenderer()
            {
                return new WysiwygEditorXHTMLRenderer(new DefaultWikiPrinter(), new MockDocumentAccessBridge(), null);
            }
        });
        factories.put("tex", new PrintRendererFactory()
        {
            public PrintRenderer createRenderer()
            {
                return new TexRenderer(new DefaultWikiPrinter());
            }
        });

        RenderingTestSuite suite = new RenderingTestSuite("Test all Parsers/Renderers", factories);

        // Text formatting
        suite.addTestsFromResource("bold/bold1", false);
        suite.addTestsFromResource("bold/bold2", false);
        suite.addTestsFromResource("bold/bold3", false);
        suite.addTestsFromResource("bold/bold4", false);
        suite.addTestsFromResource("bold/bold5", false);
        suite.addTestsFromResource("bold/bold6", false);
        suite.addTestsFromResource("bold/bold7", false);
        suite.addTestsFromResource("italic/italic1", false);
        suite.addTestsFromResource("italic/italic2", false);
        suite.addTestsFromResource("italic/italic3", false);
        suite.addTestsFromResource("italic/italic4", false);
        suite.addTestsFromResource("italic/italic5", false);
        suite.addTestsFromResource("italic/italic6", false);
        suite.addTestsFromResource("italic/italic7", false);
        suite.addTestsFromResource("underline/underline1", false);
        suite.addTestsFromResource("underline/underline2", false);
        suite.addTestsFromResource("strikedout/strikedout1", false);
        suite.addTestsFromResource("strikedout/strikedout2", false);
        suite.addTestsFromResource("strikedout/strikedout3", false);
        suite.addTestsFromResource("superscript/superscript1", false);
        suite.addTestsFromResource("subscript/subscript1", false);
        suite.addTestsFromResource("monospace/monospace1", false);
        suite.addTestsFromResource("paragraph/paragraph1", false);
        suite.addTestsFromResource("paragraph/paragraph2", false);
        suite.addTestsFromResource("paragraph/paragraph3", false);

        // Macros
        suite.addTestsFromResource("macros/macro", false);
        suite.addTestsFromResource("macros/macrohtml", true);
        suite.addTestsFromResource("macros/macronowiki", true);
        suite.addTestsFromResource("macros/velocity/macrovelocity1", true);
        suite.addTestsFromResource("macros/velocity/macrovelocity2", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml1", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml2", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml3", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml4", true);
        suite.addTestsFromResource("macros/xhtml/macroxhtml5", true);
        suite.addTestsFromResource("macros/macroid", true);
        suite.addTestsFromResource("macros/toc/macrotoc1", true);
        suite.addTestsFromResource("macros/toc/macrotoc2", true);
        suite.addTestsFromResource("macros/toc/macrotoc3", true);

        // Other
        suite.addTestsFromResource("escape/escape1", false);
        suite.addTestsFromResource("horizontalline", false);
        suite.addTestsFromResource("html", false);
        suite.addTestsFromResource("links", false);
        suite.addTestsFromResource("list/list1", false);
        suite.addTestsFromResource("list/list2", false);
        suite.addTestsFromResource("list/list3", false);
        suite.addTestsFromResource("list/list4", false);
        suite.addTestsFromResource("list/list5", false);
        suite.addTestsFromResource("section", false);

        return new PlexusTestSetup(suite);
    }
}
