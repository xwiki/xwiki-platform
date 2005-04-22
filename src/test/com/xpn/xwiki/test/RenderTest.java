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
 * Created by
 * User: Ludovic Dubost
 * Date: 27 nov. 2003
 * Time: 17:20:33
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import junit.framework.TestCase;
import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;

import java.net.URL;

public abstract class RenderTest extends TestCase {

    public XWiki xwiki;
    public XWikiContext context;

    public abstract XWikiRenderer getXWikiRenderer();

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public XWikiStoreInterface getStore() {
        return xwiki.getStore();
    }

    public void setUp() throws Exception {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context, null, false);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
        Velocity.init("velocity.properties");
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }



    public static String renderTest(XWikiRenderer renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {

        // Add a line feed at the end of source and target
        // because if one is missing at the end it will be added by the rendering engine
        if (fullmatch) {
            if (!source.endsWith("\n"))
                source =  source + "\n";
            if (!result.endsWith("\n"))
                result =  result + "\n";
        }

        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        doc.setContent(source);
        String res = renderer.render(source, doc, doc, context);
        assertTrue(renderer.getClass().toString() + " Failed Rendering of\n-------\n" + source
                + "\n-------\nto\n-------\n" + result + "\n-------\nRendered output:\n-------\n"
                + res + "\n-------\n",
                (fullmatch) ? res.equals(result) : (res.indexOf(result)!=-1));
        return res;
    }

    public static String renderTest(XWikiRenderingEngine renderer, String source, String result, boolean fullmatch, XWikiContext context) throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Main","WebHome");
        doc.setContent(source);
        return renderTest(renderer, doc, result, fullmatch, context);
    }


    public static String renderTest(XWikiRenderingEngine renderer, XWikiDocument doc, String result, boolean fullmatch, XWikiContext context) throws XWikiException {
          // Make sure we have the doc in the context
          context.put("doc", doc);
          context.put("cdoc", doc);

          // Add a line feed at the end of source and target
          // because if one is missing at the end it will be added by the rendering engine
          String source = doc.getContent();
          if (fullmatch) {
              if (!source.endsWith("\n"))
                  source =  source + "\n";
              if (!result.endsWith("\n"))
                  result =  result + "\n";
          }
          doc.setContent(source);

          // Render
          String res = renderer.renderDocument(doc, context);
          assertTrue(renderer.getClass().toString() + " Failed Rendering of\n-------\n" + source
                  + "\n-------\nto\n-------\n" + result + "\n-------\nRendered output:\n-------\n"
                  + res + "\n-------\n",
                  (fullmatch) ? res.equals(result) : (res.indexOf(result)!=-1));
          return res;
      }



    public void testWikiBaseHeadingRenderer() throws XWikiException {
        XWikiRenderer wikibase = getXWikiRenderer();
        // Test <hr>
        renderTest(wikibase, "Hello 1\n---\nHello 2",
                "Hello 1\n<hr />\nHello 2", true, context);
        // Test heading
        renderTest(wikibase, "Hello 1\n---+ Title\nHello 2",
                "<h1 id=\"Title\" >", false, context);
        renderTest(wikibase, "Hello 1\n---++ Title\nHello 2",
                "<h2 id=\"Title\" >", false, context);
    }

    public void testWikiBaseFormattingRenderer() throws XWikiException {
         XWikiRenderer wikibase = getXWikiRenderer();

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

    public void testWikiBasePreRenderer() throws XWikiException {
         XWikiRenderer wikibase = getXWikiRenderer();

        // Test formatting
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "---+ Title {pre}\n*strong*\n{/pre}",
                 "<h1 id=\"Title_0_\" >Title \n*strong*\n</h1>", false, context);
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
                 "<li> Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
                "This is a text with *one* and *two* items", false, context);
        renderTest(wikibase, "{pre}This is a text with *strong* text{/pre}",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "{pre}\nThis is a text with *strong* text\n{/pre}",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "This is a text with{pre} *strong* {/pre}text\n",
                "This is a text with *strong* text", false, context);
        renderTest(wikibase, "---+ Title {pre}\n*strong*\n{/pre}",
                 "<h1 id=\"Title_0_\" >Title \n*strong*\n</h1>", false, context);
        renderTest(wikibase, "   * Item {pre}*strong*{/pre}",
                 "<li> Item *strong*</li>", false, context);
        renderTest(wikibase, "This is a text with{pre} *one* {/pre}and{pre} *two* {/pre}items\n",
                "This is a text with *one* and *two* items", false, context);

    }

    public void testWikiBaseTabListRenderer() throws XWikiException {
         XWikiRenderer wikibase = getXWikiRenderer();
         renderTest(wikibase, "\t* List1",
                "<ul><li> List1</li>\n</ul>\n", true, context);
         renderTest(wikibase, "\t* List1\n\t* List2",
               "<ul><li> List1</li>\n<li> List2</li>\n</ul>\n", true, context);
        renderTest(wikibase, "\t* List1\n\t\t* List2",
              "<ul><li> List1</li>\n<ul><li> List2</li>\n</ul></ul>\n", true, context);

    }

    public void testWikiBaseSpaceListRenderer() throws XWikiException {
         XWikiRenderer wikibase = getXWikiRenderer();
         renderTest(wikibase, "   * List1",
                "<ul><li> List1</li>\n</ul>", true, context);
         renderTest(wikibase, "   * List1\n   * List2",
               "<ul><li> List1</li>\n<li> List2</li>\n</ul>\n", true, context);
        renderTest(wikibase, "   * List1\n      * List2",
              "<ul><li> List1</li>\n<ul><li> List2</li>\n</ul></ul>\n", true, context);

    }

    public void testWikiBaseLinkRenderer() throws XWikiException, HibernateException {
         XWikiRenderer wikibase = getXWikiRenderer();
         XWikiDocument doc = new XWikiDocument("Main","WebHome");
         context.put("doc", doc);

         renderTest(wikibase, "Test link: UnknownPage",
               ">?</a>", false, context);
         renderTest(wikibase, "Test link: WebHome",
                "Main/WebHome", false, context);
         renderTest(wikibase, "Test link: Main.WebHome",
                "Main/WebHome", false, context);

         String sclass = this.getClass().getName();
         if (sclass.indexOf("WikiWikiBaseRenderTest")==-1) {
            renderTest(wikibase, "Test link: [[Web Home]]",
               "WebHome</a>", false, context);
            renderTest(wikibase, "Test link: [[http://link/][WebHome]]",
               "<a href=\"http://link\">WebHome</a>", false, context);
         }
    }


}
