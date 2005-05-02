/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 09:23:00
 */


package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.render.XWikiRenderer;
import org.hibernate.HibernateException;



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
        renderTest(wikibase, "Hello 1\n1.1 Title\nHello 2",
            "<h3 class=\"heading-1-1\"><a id=\"Title\" name=\"Title\">Title</a></h3>", false, context);
    }

    public void testWikiBasePreRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test formatting
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "1 Title {pre}\n*strong*\n{/pre}",
        "<h3 class=\"heading-1\"><a id=\"Title+%25_0_%25\" name=\"Title+%25_0_%25\"/>Title \n*strong*\n</a></h3>", false, context);
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
        "<li>Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
        "This is a text with *one* and *two* items", false, context);
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
        "This is a text with *strong* text", false, context);
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
        "<li>Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
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

    public void testWikiBaseLinkRenderer() throws XWikiException, HibernateException {
        StoreHibernateTest.cleanUp(getHibStore(), context);
        Utils.createDoc(getHibStore(), "Main", "WebHome", context);
        XWikiRenderer wikibase = getXWikiRenderer();
        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        context.put("doc", doc);

        renderTest(wikibase, "Test link: [Web Home]",
                "view/Main/WebHome", false, context);
        renderTest(wikibase, "Test link: [Web Home]",
                "Web Home</a>", false, context);
        renderTest(wikibase, "Test link: [Web Home]",
                "view/Main/WebHome", false, context);
        renderTest(wikibase, "Test link: [Web Home12]",
                "Web Home12", false, context);
        renderTest(wikibase, "Test link: [Web Home12]",
                "edit/Main/WebHome12?parent=", false, context);
        renderTest(wikibase, "Test link: [Other Text|WebHome]",
                "Other Text", false, context);
        renderTest(wikibase, "Test link: [Other Text|WebHome]",
                "view/Main/WebHome", false, context);
        renderTest(wikibase, "Test link: [Other Text|WebHome12]",
                "Other Text", false, context);
        renderTest(wikibase, "Test link: [Other Text|WebHome12]",
                "edit/Main/WebHome12?parent=", false, context);
        renderTest(wikibase, "Test link: http://www.ludovic.org/",
                "<a href=\"http://www.ludovic.org/\">", false, context);
        renderTest(wikibase, "Test link: {link:WebHome|http://www.ludovic.org/}",
                "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, context);
        renderTest(wikibase, "Test link: {link:WebHome| http://www.ludovic.org/ }",
                "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, context);
        renderTest(wikibase, "Test link: [http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#104;ttp://www.ludovic.org/</a>", false, context);
        renderTest(wikibase, "Test link: [ludovic>http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic</a>", false, context);
        renderTest(wikibase, "Test link: [ludovic web site>http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, context);
        renderTest(wikibase, "Test link: [ludovic web site> http://www.ludovic.org/ ]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, context);
        renderTest(wikibase, "Test link: [ludovic>mailto:ludovic@xwiki.org]",
                     "<a href=\"mailto:ludovic@xwiki.org\">&#108;udovic</a>", false, context);
        }


       public String renderTestInTable(XWikiRenderer renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {
           String source2 = "{table}\na | b\ntext|" + source + "\n{table}\n";
           String res = RenderTest.renderTest(renderer, source2, result, fullmatch, context);
           assertTrue("Result should contain a table", res.indexOf("<table")!=-1);
           return res;
       }

       public void testWikiLinksInTables() throws XWikiException, HibernateException {
           StoreHibernateTest.cleanUp(getHibStore(), context);
           Utils.createDoc(getHibStore(), "Main", "WebHome", context);
           XWikiRenderer wikibase = getXWikiRenderer();
           XWikiDocument doc = new XWikiDocument("Main","WebHome");
           context.put("doc", doc);

           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "view/Main/WebHome", false, context);
           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "Web Home</a>", false, context);
           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "view/Main/WebHome", false, context);
           renderTestInTable(wikibase, "Test link: [Web Home12]",
                   "Web Home12", false, context);
           renderTestInTable(wikibase, "Test link: [Web Home12]",
                   "edit/Main/WebHome12?parent=", false, context);
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome]",
                   "Other Text", false, context);
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome]",
                   "view/Main/WebHome", false, context);
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome12]",
                   "Other Text", false, context);
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome12]",
                   "edit/Main/WebHome12?parent=", false, context);
           renderTestInTable(wikibase, "Test link: http://www.ludovic.org/",
                   "<a href=\"http://www.ludovic.org/\">", false, context);
           renderTestInTable(wikibase, "Test link: {link:WebHome|http://www.ludovic.org/}",
                   "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, context);
           renderTestInTable(wikibase, "Test link: {link:WebHome| http://www.ludovic.org/ }",
                   "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, context);
           renderTestInTable(wikibase, "Test link: [http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#104;ttp://www.ludovic.org/</a>", false, context);
           renderTestInTable(wikibase, "Test link: [ludovic>http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic</a>", false, context);
           renderTestInTable(wikibase, "Test link: [ludovic web site>http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, context);
           renderTestInTable(wikibase, "Test link: [ludovic web site> http://www.ludovic.org/ ]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, context);
           renderTestInTable(wikibase, "Test link: [ludovic>mailto:ludovic@xwiki.org]",
                        "<a href=\"mailto:ludovic@xwiki.org\">&#108;udovic</a>", false, context);
       }

       public void testHTMLCodeRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "{code}\n<html>\n{code}",
                "&#60;html&#62;", false, context);
       }

       public void testRSSRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "{rss:feed=http://www.ludovic.org/blog/index.rdf}",
                "LudoBlog", false, context);
       }

      public void testWikiBaseVirtualLinkRenderer() throws XWikiException, HibernateException {
        StoreHibernateTest.cleanUp(getHibStore(), context);
        Utils.createDoc(getHibStore(), "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(getHibStore(), context);
        Utils.createDoc(getHibStore(), "Main", "WebHome", context);
        context.setDatabase("xwikitest");


        XWikiRenderer wikibase = getXWikiRenderer();
        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        context.put("doc", doc);

        String res = renderTest(wikibase, "Test link: [xwikitest2:Main.WebHome]",
                "127.0.0.1", false, context);
        assertTrue("Cannot find view link", res.indexOf("view/Main/WebHome")!=-1 );
        res = renderTest(wikibase, "Test link: [xwikitest2:Main.WebHome12]",
                  "127.0.0.1", false, context);
        assertTrue("Cannot find edit link", res.indexOf("edit/Main/WebHome")!=-1 );
      }


}
