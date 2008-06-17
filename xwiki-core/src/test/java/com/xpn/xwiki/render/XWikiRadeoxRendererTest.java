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
package com.xpn.xwiki.render;

import java.net.URL;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.core.Constraint;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiRadeoxRenderer}.
 * 
 * @version $Id$
 */
public class XWikiRadeoxRendererTest extends MockObjectTestCase
{
    private XWikiContext context;

    private XWikiRadeoxRenderer renderer;

    private Mock mockXWiki;

    private Mock mockDocument;

    private Mock mockContentDocument;

    private XWikiDocument document;

    private XWikiDocument contentDocument;

    protected void setUp()
    {
        this.renderer = new XWikiRadeoxRenderer();
        this.context = new XWikiContext();

        this.mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        this.context.setWiki((XWiki) this.mockXWiki.proxy());

        this.mockContentDocument = mock(XWikiDocument.class);
        this.contentDocument = (XWikiDocument) this.mockContentDocument.proxy();

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        // This is required just to return the current space...
        Mock mockCurrentDocument = mock(XWikiDocument.class);
        mockCurrentDocument.stubs().method("getSpace").will(returnValue("Main"));
        mockCurrentDocument.stubs().method("getFullName").will(returnValue("Main.WebHome"));
        this.context.setDoc((XWikiDocument) mockCurrentDocument.proxy());
    }

    public void testRenderWithSimpleText()
    {
        String result = renderer.render("Simple content", contentDocument, document, context);

        assertEquals("Simple content", result);
    }

    /**
     * @todo this test is too complex and show that the rendering API is not right...
     */
    public void testRenderLinkToNewPage() throws Exception
    {
        this.mockXWiki.expects(once()).method("exists").with(eq("Main.newlink"), ANYTHING).will(returnValue(false));
        this.mockXWiki.expects(once()).method("exists").with(eq("Main.new link"), ANYTHING).will(returnValue(false));
        this.mockXWiki.expects(once()).method("getEditorPreference").will(returnValue("text"));

        Mock mockUrlFactory = mock(XWikiURLFactory.class);
        mockUrlFactory.expects(once()).method("createURL").with(
            new Constraint[] {eq("Main"), eq("new link"), eq("edit"), eq("parent=Main.WebHome"), ANYTHING, ANYTHING})
            .will(returnValue(new URL("http://server.com/Main/new link")));
        mockUrlFactory.expects(atLeastOnce()).method("getURL").will(returnValue("/Main/new link"));
        this.context.setURLFactory((XWikiURLFactory) mockUrlFactory.proxy());

        String result = renderer.render("This is a [new link]", contentDocument, document, context);

        assertEquals("This is a <a class=\"wikicreatelink\" href=\"/Main/new link\">"
            + "<span class=\"wikicreatelinktext\">new link</span>" + "<span class=\"wikicreatelinkqm\">?</span></a>",
            result);
    }

    public void testRenderStyleMacro() throws Exception
    {
        String result =
            renderer.render("{style:type=div|align=justify}Hello{style}", contentDocument, document, context);
        assertEquals("<div align=\"justify\" style=\"\" >Hello</div>", result);
    }

    public void testRenderStyleMacroNotImbricated() throws Exception
    {
        String result =
            renderer
                .render(
                    "{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}. How fun.",
                    contentDocument, document, context);
        assertEquals(
            "<span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>. How fun.",
            result);
    }

    public void testRenderStyleMacroNotImbricatedInImbricated() throws Exception
    {
        String result =
            renderer
                .render(
                    "{style:type=div|align=justify}{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}.{style} How fun.",
                    contentDocument, document, context);
        assertEquals(
            "<div align=\"justify\" style=\"\" ><span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>.</div> How fun.",
            result);
    }

    public void testRenderStyleMacroImbricated() throws Exception
    {
        String result =
            renderer
                .render(
                    "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style}",
                    contentDocument, document, context);
        assertEquals(
            "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div>",
            result);
    }

    public void testRenderStyleMacroImbricated2() throws Exception
    {
        String result =
            renderer
                .render(
                    "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style} and this is very fun {style}",
                    contentDocument, document, context);
        assertEquals(
            "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div> and this is very fun <span style=\"\" ></span>",
            result);
    }

    public void testRenderParagraph() throws Exception
    {
        String result = renderer.render("a\n\nb", contentDocument, document, context);
        assertEquals("a<p/>\nb", result);
    }

    public void testRenderOneParagraphForSeveralNewlines() throws Exception
    {
        String result = renderer.render("a\n\n\n\n\nb", contentDocument, document, context);
        assertEquals("a<p/>\nb", result);
    }

    public void testRenderParagraphIgnoresSpaces() throws Exception
    {
        String result = renderer.render("a\n  \t\n  b", contentDocument, document, context);
        assertEquals("a<p/>\n  b", result);
    }

    public void testRenderParagraphWithBr() throws Exception
    {
        String result = renderer.render("a\\\\\n\n\nb", contentDocument, document, context);
        assertEquals("a<br/><p/>\nb", result);
    }

    public void testRenderNewline() throws Exception
    {
        String result = renderer.render("a\\\\b", contentDocument, document, context);
        assertEquals("a<br/>b", result);
    }

    public void testRenderNewlineWithCarriageReturn() throws Exception
    {
        String result = renderer.render("a\\\\\nb", contentDocument, document, context);
        assertEquals("a<br/>b", result);
    }

    public void testRenderTwoNewline() throws Exception
    {
        String result = renderer.render("a\\\\\\\\b", contentDocument, document, context);
        assertEquals("a<br/><br/>b", result);
    }

    public void testRenderTwoNewlineWithCarriageReturn() throws Exception
    {
        String result = renderer.render("a\\\\\\\\\nb", contentDocument, document, context);
        assertEquals("a<br/><br/>b", result);
    }

    public void testRenderThreeNewline() throws Exception
    {
        String result = renderer.render("a\\\\\\\\\\\\b", contentDocument, document, context);
        assertEquals("a<br/><br/><br/>b", result);
    }

    public void testRenderEncodedBackslash() throws Exception
    {
        String result = renderer.render("\\\\\\", contentDocument, document, context);
        assertEquals("&#92;", result);
    }

    public void testRenderEscapedCharacters() throws Exception
    {
        String result = renderer.render("\\[NotALink\\]", contentDocument, document, context);
        assertEquals("&#91;NotALink&#93;", result);
    }
}
