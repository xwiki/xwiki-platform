/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.render.XWikiRenderer;

public class RadeoxRenderTest extends AbstractRenderTest {

    public XWikiRenderer getXWikiRenderer() {
        return new XWikiRadeoxRenderer();
    }

    public void testWikiBaseFormattingRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();

        // Test formatting
        renderTest(wikibase, "Hello 1\nThis is a text with *strong* text\nHello 2",
                "<strong class=\"strong\">", false, getXWikiContext());
        renderTest(wikibase, "Hello 1\n*strong*\nHello 2",
                "<strong class=\"strong\">strong</strong>", false, getXWikiContext());

        renderTest(wikibase, "Hello 1\nThis is a text with __bold__ text\nHello 2",
                "<b class=\"bold\">", false, getXWikiContext());
        renderTest(wikibase, "Hello 1\n__bold__\nHello 2",
                "<b class=\"bold\">bold</b>", false, getXWikiContext());

    }

    public void testWikiBaseHeadingRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test <hr>
        renderTest(wikibase, "Hello 1\n----\nHello 2",
                "Hello 1\n<hr class=\"line\"/>\nHello 2", true, getXWikiContext());
        // Test heading
        renderTest(wikibase, "Hello 1\n1 Title\nHello 2",
                "<h2 class=\"heading-1\">", false, getXWikiContext());
        renderTest(wikibase, "Hello 1\n1.1 Title\nHello 2",
                "<h3 class=\"heading-1-1\">", false, getXWikiContext());
        renderTest(wikibase, "Hello 1\n1.1 Title\nHello 2",
            "<h3 class=\"heading-1-1\"><span id=\"HTitle\">Title</span></h3>", false, getXWikiContext());
    }

    public void testWikiBasePreRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test formatting
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "1 Title {pre}\n*strong*\n{/pre}",
        "<h2 class=\"heading-1\"><span id=\"HTitle25025\">Title \n*strong*\n</span></h2>", false, getXWikiContext());
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
        "<li>Item *strong*</li>", false, getXWikiContext());
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
        "This is a text with *one* and *two* items", false, getXWikiContext());
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
        "This is a text with *strong* text", false, getXWikiContext());
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
        "<li>Item *strong*</li>", false, getXWikiContext());
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
        "This is a text with *one* and *two* items", false, getXWikiContext());
    }

    public void testWikiBaseTabListRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
    }

    public void testWikiBaseSpaceListRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "* List1",
                "<ul class=\"star\">\n<li>List1</li>\n</ul>", false, getXWikiContext());
        renderTest(wikibase, "* List1\n* List2",
                "<ul class=\"star\">\n<li>List1</li>\n<li>List2</li>\n</ul>", false, getXWikiContext());
    }

    public void testWikiBaseLinkRenderer() throws XWikiException, HibernateException {
        Utils.createDoc(getXWiki().getHibernateStore(), "Main", "WebHome", getXWikiContext());
        XWikiRenderer wikibase = getXWikiRenderer();
        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        getXWikiContext().put("doc", doc);

        renderTest(wikibase, "Test link: [Web Home]",
                "Main/", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Web Home]",
                "Web Home</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Web Home]",
                "Main/", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Web Home12]",
                "Web+Home12", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Web Home12]",
                "edit/Main/Web+Home12?parent=", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Other Text|WebHome]",
                "Other Text", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Other Text|WebHome]",
                "Main/", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Other Text|WebHome12]",
                "Other Text", false, getXWikiContext());
        renderTest(wikibase, "Test link: [Other Text|WebHome12]",
                "edit/Main/WebHome12?parent=", false, getXWikiContext());
        renderTest(wikibase, "Test link: http://www.ludovic.org/",
                "<a href=\"http://www.ludovic.org/\">", false, getXWikiContext());
        renderTest(wikibase, "Test link: {link:WebHome|http://www.ludovic.org/}",
                "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: {link:WebHome| http://www.ludovic.org/ }",
                "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#104;ttp://www.ludovic.org/</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [ludovic>http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [ludovic web site>http://www.ludovic.org/]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [ludovic web site> http://www.ludovic.org/ ]",
                     "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [ludovic>mailto:ludovic@xwiki.org]",
                     "<a href=\"mailto:ludovic@xwiki.org\">&#108;udovic</a>", false, getXWikiContext());
        
        // test internal links
        renderTest(wikibase, "Test link: [#anchorname]",
            "<a href=\"#anchorname\">anchorname</a>", false, getXWikiContext());
        renderTest(wikibase, "Test link: [internal link>#anchorname]",
            "<a href=\"#anchorname\">internal link</a>", false, getXWikiContext());
        }


       public String renderTestInTable(XWikiRenderer renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {
           String source2 = "{table}\na | b\ntext|" + source + "\n{table}\n";
           String res = AbstractRenderTest.renderTest(renderer, source2, result, fullmatch, context);
           assertTrue("Result should contain a table", res.indexOf("<table")!=-1);
           return res;
       }

       public void testWikiLinksInTables() throws XWikiException, HibernateException {
           Utils.createDoc(getXWiki().getHibernateStore(), "Main", "WebHome", getXWikiContext());
           XWikiRenderer wikibase = getXWikiRenderer();
           XWikiDocument doc = new XWikiDocument("Main","WebHome");
           getXWikiContext().put("doc", doc);

           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "Main/", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "Web Home</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Web Home]",
                   "Main/", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Web Home12]",
                   "Web Home12", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Web Home12]",
                   "edit/Main/Web+Home12?parent=", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome]",
                   "Other Text", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome]",
                   "Main/", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome12]",
                   "Other Text", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [Other Text>WebHome12]",
                   "edit/Main/WebHome12?parent=", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: http://www.ludovic.org/",
                   "<a href=\"http://www.ludovic.org/\">", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: {link:WebHome|http://www.ludovic.org/}",
                   "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: {link:WebHome| http://www.ludovic.org/ }",
                   "<a href=\"http://www.ludovic.org/\">WebHome</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#104;ttp://www.ludovic.org/</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [ludovic>http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [ludovic web site>http://www.ludovic.org/]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [ludovic web site> http://www.ludovic.org/ ]",
                        "<a href=\"http://www.ludovic.org/\">&#108;udovic web site</a>", false, getXWikiContext());
           renderTestInTable(wikibase, "Test link: [ludovic>mailto:ludovic@xwiki.org]",
                        "<a href=\"mailto:ludovic@xwiki.org\">&#108;udovic</a>", false, getXWikiContext());
       }

       public void testHTMLCodeRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "{code}\n<html>\n{code}",
                "&#60;html&#62;", false, getXWikiContext());
       }

       public void testRSSRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        renderTest(wikibase, "{rss:feed=http://www.ludovic.org/blog/index.rdf}",
                "LudoBlog", false, getXWikiContext());
       }

      public void testWikiBaseVirtualLinkRenderer() throws XWikiException, HibernateException {
        Utils.createDoc(getXWiki().getHibernateStore(), "XWiki", "XWikiServerXwikitest2", getXWikiContext());
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", getXWikiContext());

        getXWikiContext().setDatabase("xwikitest2");
        // TODO: Split into several tests if you need a new clean up.
        StoreHibernateTest.cleanUp(getXWiki().getHibernateStore(), false, true, getXWikiContext());
        Utils.createDoc(getXWiki().getHibernateStore(), "Main", "WebHome", getXWikiContext());
        getXWikiContext().setDatabase("xwikitest");

        XWikiRenderer wikibase = getXWikiRenderer();
        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        getXWikiContext().put("doc", doc);

        String res = renderTest(wikibase, "Test link: [xwikitest2:Main.WebHome]",
                "127.0.0.1", false, getXWikiContext());
        assertTrue("Cannot find view link", res.indexOf("Main/")!=-1 );
        res = renderTest(wikibase, "Test link: [xwikitest2:Main.WebHome12]",
                  "127.0.0.1", false, getXWikiContext());
        assertTrue("Cannot find edit link", res.indexOf("edit/Main/")!=-1 );
      }
}
