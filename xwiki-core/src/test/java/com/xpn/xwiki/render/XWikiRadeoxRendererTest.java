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
import org.jmock.core.Constraint;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiURLFactory;
import org.xwiki.model.reference.DocumentReference;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiRadeoxRenderer}.
 * 
 * @version $Id$
 */
public class XWikiRadeoxRendererTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiRadeoxRenderer renderer;

    private Mock mockXWiki;

    private Mock mockDocument;

    private Mock mockContentDocument;

    private XWikiDocument document;

    private XWikiDocument contentDocument;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.renderer = new XWikiRadeoxRenderer();

        this.mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        this.mockXWiki.stubs().method("Param").will(returnValue(""));
        getContext().setWiki((XWiki) this.mockXWiki.proxy());

        this.mockContentDocument = mock(XWikiDocument.class);
        this.contentDocument = (XWikiDocument) this.mockContentDocument.proxy();

        this.mockDocument = mock(XWikiDocument.class);
        this.document = (XWikiDocument) this.mockDocument.proxy();

        // This is required just to return the current space...
        Mock mockCurrentDocument = mock(XWikiDocument.class);
        mockCurrentDocument.stubs().method("getDocumentReference").will(returnValue(
            new DocumentReference("xwiki", "Main", "WebHome")));
        mockCurrentDocument.stubs().method("getFullName").will(returnValue("Main.WebHome"));
        getContext().setDoc((XWikiDocument) mockCurrentDocument.proxy());
    }

    public void testRenderWithSimpleText()
    {
        String result = this.renderer.render("Simple content", this.contentDocument, this.document, getContext());

        assertEquals("Simple content", result);
    }

    /**
     * @todo this test is too complex and show that the rendering API is not right...
     */
    public void testRenderLinkToNewPage() throws Exception
    {
        this.mockXWiki.expects(once()).method("exists").with(eq("Main.newlink"), ANYTHING).will(returnValue(false));
        this.mockXWiki.expects(once()).method("exists").with(eq("Main.new link"), ANYTHING).will(returnValue(false));

        Mock mockUrlFactory = mock(XWikiURLFactory.class);
        mockUrlFactory.expects(once()).method("createURL").with(
            new Constraint[] {eq("Main"), eq("new link"), eq("edit"), eq("parent=Main.WebHome"), ANYTHING, ANYTHING})
            .will(returnValue(new URL("http://server.com/Main/new link")));
        mockUrlFactory.expects(atLeastOnce()).method("getURL").will(returnValue("/Main/new link"));
        getContext().setURLFactory((XWikiURLFactory) mockUrlFactory.proxy());

        String result = this.renderer.render("This is a [new link]", this.contentDocument, this.document, getContext());

        assertEquals("This is a <a class=\"wikicreatelink\" href=\"/Main/new link\">"
            + "<span class=\"wikicreatelinktext\">new link</span>" + "<span class=\"wikicreatelinkqm\">?</span></a>",
            result);
    }

    /**
     * Test that the parent is correctly escaped for links to non-existing documents.
     */
    public void testEscapedParentForLinkToNewPage() throws Exception
    {
        this.mockXWiki.expects(once()).method("exists").with(eq("A+ B.newlink"), ANYTHING).will(returnValue(false));
        this.mockXWiki.expects(once()).method("exists").with(eq("A+ B.new link"), ANYTHING).will(returnValue(false));

        Mock mockUrlFactory = mock(XWikiURLFactory.class);
        mockUrlFactory.expects(once()).method("createURL").with(
            new Constraint[] {eq("A+ B"), eq("new link"), eq("edit"),
            eq("parent=A%2B+B.C%23+Examples+%26+Libs%3F+No%2C+I+prefer+C%2B%2B"), ANYTHING, ANYTHING})
            .will(returnValue(new URL("http://server.com/A%2B%20B/new link")));
        mockUrlFactory.expects(atLeastOnce()).method("getURL").will(returnValue("/A%2B+B/new link"));
        getContext().setURLFactory((XWikiURLFactory) mockUrlFactory.proxy());

        Mock mockCurrentDocument = mock(XWikiDocument.class);
        mockCurrentDocument.stubs().method("getDocumentReference").will(returnValue(
            new DocumentReference("xwiki", "A+ B", "C# Examples & Libs? No, I prefer C++")));
        mockCurrentDocument.stubs().method("getFullName")
            .will(returnValue("A+ B.C# Examples & Libs? No, I prefer C++"));
        getContext().setDoc((XWikiDocument) mockCurrentDocument.proxy());

        String result = this.renderer.render("This is a [new link]", this.contentDocument, this.document, getContext());

        assertEquals("This is a <a class=\"wikicreatelink\" href=\"/A%2B+B/new link\">"
            + "<span class=\"wikicreatelinktext\">new link</span>" + "<span class=\"wikicreatelinkqm\">?</span></a>",
            result);
    }

    public void testRenderStyleMacro() throws Exception
    {
        String result =
            this.renderer.render("{style:type=div|align=justify}Hello{style}", this.contentDocument, this.document,
                getContext());
        assertEquals("<div align=\"justify\" style=\"\" >Hello</div>", result);
    }

    public void testRenderStyleMacroNotImbricated() throws Exception
    {
        String result =
            this.renderer
                .render(
                    "{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}. How fun.",
                    this.contentDocument, this.document, getContext());
        assertEquals(
            "<span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>. How fun.",
            result);
    }

    public void testRenderStyleMacroNotImbricatedInImbricated() throws Exception
    {
        String result =
            this.renderer
                .render(
                    "{style:type=div|align=justify}{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}.{style} How fun.",
                    this.contentDocument, this.document, getContext());
        assertEquals(
            "<div align=\"justify\" style=\"\" ><span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>.</div> How fun.",
            result);
    }

    public void testRenderStyleMacroImbricated() throws Exception
    {
        String result =
            this.renderer
                .render(
                    "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style}",
                    this.contentDocument, this.document, getContext());
        assertEquals(
            "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div>",
            result);
    }

    public void testRenderStyleMacroImbricated2() throws Exception
    {
        String result =
            this.renderer
                .render(
                    "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style} and this is very fun {style}",
                    this.contentDocument, this.document, getContext());
        assertEquals(
            "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div> and this is very fun <span style=\"\" ></span>",
            result);
    }

    public void testRenderParagraph() throws Exception
    {
        String result = this.renderer.render("a\n\nb", this.contentDocument, this.document, getContext());
        assertEquals("a<p/>\nb", result);
    }

    public void testRenderOneParagraphForSeveralNewlines() throws Exception
    {
        String result = this.renderer.render("a\n\n\n\n\nb", this.contentDocument, this.document, getContext());
        assertEquals("a<p/>\nb", result);
    }

    public void testRenderParagraphIgnoresSpaces() throws Exception
    {
        String result = this.renderer.render("a\n  \t\n  b", this.contentDocument, this.document, getContext());
        assertEquals("a<p/>\n  b", result);
    }

    public void testRenderParagraphWithBr() throws Exception
    {
        String result = this.renderer.render("a\\\\\n\n\nb", this.contentDocument, this.document, getContext());
        assertEquals("a<br/><p/>\nb", result);
    }

    public void testRenderNewline() throws Exception
    {
        String result = this.renderer.render("a\\\\b", this.contentDocument, this.document, getContext());
        assertEquals("a<br/>b", result);
    }

    public void testRenderNewlineWithCarriageReturn() throws Exception
    {
        String result = this.renderer.render("a\\\\\nb", this.contentDocument, this.document, getContext());
        assertEquals("a<br/>b", result);
    }

    public void testRenderTwoNewline() throws Exception
    {
        String result = this.renderer.render("a\\\\\\\\b", this.contentDocument, this.document, getContext());
        assertEquals("a<br/><br/>b", result);
    }

    public void testRenderTwoNewlineWithCarriageReturn() throws Exception
    {
        String result = this.renderer.render("a\\\\\\\\\nb", this.contentDocument, this.document, getContext());
        assertEquals("a<br/><br/>b", result);
    }

    public void testRenderThreeNewline() throws Exception
    {
        String result = this.renderer.render("a\\\\\\\\\\\\b", this.contentDocument, this.document, getContext());
        assertEquals("a<br/><br/><br/>b", result);
    }

    public void testRenderEncodedBackslash() throws Exception
    {
        String result = this.renderer.render("\\\\\\", this.contentDocument, this.document, getContext());
        assertEquals("&#92;", result);
    }

    public void testRenderEscapedCharacters() throws Exception
    {
        String result = this.renderer.render("\\[NotALink\\]", this.contentDocument, this.document, getContext());
        assertEquals("&#91;NotALink&#93;", result);
    }

    public void testTable() throws Exception
    {
        String result = this.renderer.render("{table}\nA\n{table}", this.contentDocument, this.document, getContext());
        assertEquals(
            "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A</th></tr></table>",
            result);
    }

    public void testTableEmptyTable() throws Exception
    {
        String result = this.renderer.render("{table}\n{table}", this.contentDocument, this.document, getContext());
        assertEquals("<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr></tr></table>",
            result);
    }

    public void testTableWithCR() throws Exception
    {
        String result =
            this.renderer.render("{table}\nA\\\\B\n{table}", this.contentDocument, this.document, getContext());
        assertEquals(
            "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/>B</th></tr></table>",
            result);
    }

    public void testTableWithCRWithSpace() throws Exception
    {
        String result =
            this.renderer.render("{table}\nA\\\\ \n{table}", this.contentDocument, this.document, getContext());
        assertEquals(
            "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/></th></tr></table>",
            result);
    }

    public void testTableWithCRWithoutSpace() throws Exception
    {
        String result =
            this.renderer.render("{table}\nA\\\\\n{table}", this.contentDocument, this.document, getContext());
        assertEquals(
            "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/></th></tr></table>",
            result);
    }

    public void testMacrosWithWikiMarkupInUrl() throws Exception
    {
        String result =
            this.renderer.render("{image:http://www.some.server/__not__underlined.png}", this.contentDocument,
                this.document, getContext());
        assertTrue(result.indexOf("<em") == -1);
        result =
            this.renderer.render("{attach:http://www.some.server/__not__underlined.png}", this.contentDocument,
                this.document, getContext());
        assertTrue(result.indexOf("a href=\"http://www.some.server/__not__underlined.png") != -1);
        result =
            this.renderer.render("{attach:this *is* __underlined__|http://www.some.server/__not__underlined.png}",
                this.contentDocument, this.document, getContext());
        assertTrue(result.indexOf("<strong>is</strong>") != -1);
    }

    public void testUrlsWithWikiMarkup() throws Exception
    {
        String result =
            this.renderer
                .render("http://www.xwiki.org/__some__URL~~with~~markup\n"
                    + "[http://www.xwiki.org/__some__URL~~with~~markup]", this.contentDocument, this.document,
                    getContext());
        assertTrue(result.indexOf("<em>") == -1);
        assertTrue(result.indexOf("wikiexternallink") != -1);
    }

    /**
     * Tests that the java syntax highlighting for the old {code} macro behaves properly when there's an unclosed quote:
     * no stack overflow, reasonable rendering time, no thrown exceptions.
     */
    public void testJavaCodeFilterWithUnclosedQuote()
    {
        StringBuffer source = new StringBuffer("{code}private static final String S = \"This is a valid string\";\n");
        source.append("Unclosed quote: \"\n");
        for (int i = 0; i < 30; ++i) {
            source.append("private static final double D = 2.0;\n");
        }
        source.append("{code}");
        long startTime = System.currentTimeMillis();
        try {
            String result = this.renderer.render(source.toString(), this.contentDocument, this.document, getContext());
            // If a stack overflow occurs during rendering, then the valid quotes won't be recognized.
            assertTrue("Failed to detect strings", result.indexOf("quote\">") != -1);
        } catch (Throwable ex) {
            fail("Failed rendering: " + ex.getMessage());
        }
        // This test should definitely take less than a minute.
        assertTrue("Rendering took too much time", System.currentTimeMillis() - startTime < 60000);
    }
}
