/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 27 nov. 2003
 * Time: 17:20:33
 */
package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.render.*;


public class RenderTest extends TestCase {

    private static XWiki xwiki;
    private static XWikiContext context;

    public RenderTest() throws XWikiException {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
    }

    public static void renderTest(XWikiRenderer renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {

        // Add a line feed at the end of source and target
        // because if one is missing at the end it will be added by the rendering engine
        if (fullmatch) {
            if (!source.endsWith("\n"))
                source =  source + "\n";
            if (!result.endsWith("\n"))
                result =  result + "\n";
        }

        XWikiDocInterface doc = new XWikiSimpleDoc("Main","WebHome");
        doc.setContent(source);
        String res = renderer.render(source, doc, context);
        assertTrue(renderer.getClass().toString() + " Failed Rendering of\n-------\n" + source
                + "\n-------\nto\n-------\n" + result + "\n-------\nRendered output:\n-------\n"
                + res + "\n-------\n",
                (fullmatch) ? res.equals(result) : (res.indexOf(result)!=-1));
    }

    public static void renderTest(XWikiRenderingEngine renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {

        // Add a line feed at the end of source and target
        // because if one is missing at the end it will be added by the rendering engine
        if (fullmatch) {
            if (!source.endsWith("\n"))
                source =  source + "\n";
            if (!result.endsWith("\n"))
                result =  result + "\n";
        }

        XWikiDocInterface doc = new XWikiSimpleDoc("Main","WebHome");
        doc.setContent(source);
        String res = renderer.renderDocument(doc, context);
        assertTrue(renderer.getClass().toString() + " Failed Rendering of\n-------\n" + source
                + "\n-------\nto\n-------\n" + result + "\n-------\nRendered output:\n-------\n"
                + res + "\n-------\n",
                (fullmatch) ? res.equals(result) : (res.indexOf(result)!=-1));
    }


    public void testWikiBaseHeadingRenderer() throws XWikiException {
        XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
        // Test <hr>
        renderTest(wikibase, "Hello 1\n---\nHello 2",
                "Hello 1\n<hr />\nHello 2", true, context);
        // Test heading
        renderTest(wikibase, "Hello 1\n---+ Title\nHello 2",
                "<h1>", false, context);
        renderTest(wikibase, "Hello 1\n---++ Title\nHello 2",
                "<h2>", false, context);
    }

    public void testWikiBaseFormattingRenderer() throws XWikiException {
         XWikiRenderer wikibase = new XWikiWikiBaseRenderer();

        // Test formatting
        renderTest(wikibase, "Hello 1\nThis is a text with *strong* text\nHello 2",
                "<strong>", false, context);
        renderTest(wikibase, "Hello 1\n*strong*\nHello 2",
                "<strong>strong</strong>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with _em_ text\nHello 2",
                "<em>", false, context);
        renderTest(wikibase, "Hello 1\n_em_\nHello 2",
                "<em>em</em>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with __strong__ text\nHello 2",
                "<strong><em>", false, context);
        renderTest(wikibase, "Hello 1\n__strong em__\nHello 2",
                "<strong><em>strong em</em></strong>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with =fixed= text\nHello 2",
                "<code>", false, context);
        renderTest(wikibase, "Hello 1\n=fixed=\nHello 2",
                "<code>fixed</code>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with ==boldfixed== text\nHello 2",
                "<code><b>", false, context);
        renderTest(wikibase, "Hello 1\n==bold fixed==\nHello 2",
                "<code><b>bold fixed</b></code>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with ==bold fixed with one space== text\nHello 2",
                "<code><b>bold fixed with one space</b></code>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with ==bold  fixed   with    multiple     spaces== text\nHello 2",
                "<code><b>bold&nbsp; fixed&nbsp; &nbsp;with&nbsp; &nbsp; multiple&nbsp; &nbsp; &nbsp;spaces</b></code>", false, context);
        renderTest(wikibase, "Hello 1\nThis is a text with ==Hello if (5 == 6) then let's finish== text\nHello 2",
                "<code><b>Hello if (5 == 6) then let's finish</b></code>", false, context);
    }

    public void testWikiBaseTabListRenderer() throws XWikiException {
         XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
         renderTest(wikibase, "\t* List1",
                "<ul><li> List1</li>\n</ul>\n", true, context);
         renderTest(wikibase, "\t* List1\n\t* List2",
               "<ul><li> List1</li>\n<li> List2</li>\n</ul>\n", true, context);
        renderTest(wikibase, "\t* List1\n\t\t* List2",
              "<ul><li> List1</li>\n<ul><li> List2</li>\n</ul></ul>\n", true, context);

    }

    public void testWikiBaseSpaceListRenderer() throws XWikiException {
         XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
         renderTest(wikibase, "   * List1",
                "<ul><li> List1</li>\n</ul>", true, context);
         renderTest(wikibase, "   * List1\n   * List2",
               "<ul><li> List1</li>\n<li> List2</li>\n</ul>\n", true, context);
        renderTest(wikibase, "   * List1\n      * List2",
              "<ul><li> List1</li>\n<ul><li> List2</li>\n</ul></ul>\n", true, context);

    }

    public void testWikiBaseLinkRenderer() throws XWikiException {
         XWikiRenderer wikibase = new XWikiWikiBaseRenderer();
         XWikiDocInterface doc = new XWikiSimpleDoc("Main","WebHome");
         context.put("doc", doc);

         renderTest(wikibase, "Test link: UnknownPage",
               ">?</a>", false, context);
         renderTest(wikibase, "Test link: WebHome",
                "Main/WebHome", false, context);
         renderTest(wikibase, "Test link: Main.WebHome",
                "Main/WebHome", false, context);
     /*    renderTest(wikibase, "Test link: [[Web Home]]",
               "WebHome</a>", false, context);
         renderTest(wikibase, "Test link: [[http://link/][WebHome]]",
               "<a href=\"http://link\">WebHome</a>", false, context);
     */
    }

    public void testVelocityRenderer() throws XWikiException {
        XWikiRenderer wikibase = new XWikiVelocityRenderer();

        renderTest(wikibase, "#set( $foo = \"Velocity\" )\nHello $foo World!",
                "Hello Velocity World!", true, context);
        renderTest(wikibase, "Test: #include( \"view.pm\" )",
                "Test: #include", false, context);
        renderTest(wikibase, "Test: #INCLUDE( \"view.pm\" )",
                "Test: #INCLUDE", false, context);

        renderTest(wikibase, "#set( $count = 0 )\n#if ( $count == 1)\nHello1\n#else\nHello2\n#end\n",
                "Hello2", true, context);
    }

    public void testRenderingEngine() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        renderTest(wikiengine, "#set( $count = 0 )\n#if ( $count == 1)\n *Hello1* \n#else\n *Hello2* \n#end\n",
                " <strong>Hello2</strong> ", true, context);
    }
}
