package com.xpn.xwiki.test;

import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;


/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 09:23:00
 * To change this template use File | Settings | File Templates.
 */
public class RadeoxRenderTest  extends RenderTest {

    public XWikiRenderer getXWikiRenderer() {
        return new XWikiRadeoxRenderer();
    }

    public void testWikiBaseFormattingRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();

        // Test formatting
        renderTest(wikibase, "Hello 1\nThis is a text with *strong* text\nHello 2",
                "<strong class=\"strong\">", false, context);
        renderTest(wikibase, "Hello 1\n*strong*\nHello 2",
                "<strong class=\"strong\">strong</strong>", false, context);

        renderTest(wikibase, "Hello 1\nThis is a text with __bold__ text\nHello 2",
                "<b class=\"bold\">", false, context);
        renderTest(wikibase, "Hello 1\n__bold__\nHello 2",
                "<b class=\"bold\">bold</b>", false, context);

    }

    public void testWikiBaseHeadingRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test <hr>
        renderTest(wikibase, "Hello 1\n----\nHello 2",
                "Hello 1\n<hr class=\"line\"/>\nHello 2", true, context);
        // Test heading
        renderTest(wikibase, "Hello 1\n1 Title\nHello 2",
                "<h3 class=\"heading-1\">", false, context);
        renderTest(wikibase, "Hello 1\n1.1 Title\nHello 2",
                "<h3 class=\"heading-1-1\">", false, context);
    }

    public void testWikiBasePreRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test formatting
        renderTest(wikibase, "<pre>This is a text with *strong* text</pre>",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "<pre>\nThis is a text with *strong* text\n</pre>",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with<pre> *strong* </pre>text\n",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "1 Title <pre>\n*strong*\n</pre>",
        "<h3 class=\"heading-1\">Title \n*strong*\n</h3>", false, context);
        renderTest(wikibase, "   * Item <pre>*strong*</pre>",
        "<li>Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with<pre> *one* </pre>and<pre> *two* </pre>items\n",
        "This is a text with *one* and *two* items", false, context);
        renderTest(wikibase, "<PrE>This is a text with *strong* text</PrE>",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "<PrE>\nThis is a text with *strong* text\n</PrE>",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with<PrE> *strong* </PrE>text\n",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "1 Title <PrE>\n*strong*\n</PrE>",
        "<h3 class=\"heading-1\">Title \n*strong*\n</h3>", false, context);
        renderTest(wikibase, "   * Item <PrE>*strong*</PrE>",
        "<li>Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with<PrE> *one* </PrE>and<PrE> *two* </PrE>items\n",
        "This is a text with *one* and *two* items", false, context);
    }

    public void testWikiBaseTabListRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
    }

    public void testWikiBaseSpaceListRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "* List1",
                "<ul class=\"star\">\n<li>List1</li>\n</ul>", false, context);
        renderTest(wikibase, "* List1\n* List2",
                "<ul class=\"star\">\n<li>List1</li>\n<li>List2</li>\n</ul>", false, context);
    }

    public void testWikiBaseLinkRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        XWikiDocInterface doc = new XWikiSimpleDoc("Main","WebHome");
        context.put("doc", doc);

        renderTest(wikibase, "Test link: [Web Home]",
                "Main/WebHome", false, context);
        renderTest(wikibase, "Test link: [Web Home]",
                "Web Home</a>", false, context);
        renderTest(wikibase, "Test link: http://www.ludovic.org/",
                "<a href=\"http://www.ludovic.org/\">", false, context);
        renderTest(wikibase, "Test link: {link:WebHome|http://www.ludovic.org/}",
                "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, context);    }
}
