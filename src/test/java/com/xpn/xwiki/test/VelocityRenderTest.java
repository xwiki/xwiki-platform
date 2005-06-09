/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.hibernate.HibernateException;

public class VelocityRenderTest extends HibernateTestCase {

        public void testVelocityRenderer() throws XWikiException {
            XWikiRenderer wikibase = new XWikiVelocityRenderer();

            RenderTest.renderTest(wikibase, "#set( $foo = \"Velocity\" )\nHello $foo World!",
                    "Hello Velocity World!", true, getXWikiContext());
            RenderTest.renderTest(wikibase, "Test: #include( \"view.pm\" )",
                    "Test: #include", false, getXWikiContext());
            RenderTest.renderTest(wikibase, "Test: #INCLUDE( \"view.pm\" )",
                    "Test: #INCLUDE", false, getXWikiContext());

            RenderTest.renderTest(wikibase, "#set( $count = 0 )\n#if ( $count == 1)\nHello1\n#else\nHello2\n#end\n",
                    "Hello2", true, getXWikiContext());
        }

        public void testRenderingEngine() throws XWikiException {
            XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(getXWiki(), getXWikiContext());
            RenderTest.renderTest(wikiengine, "#set( $count = 0 )\n#if ( $count == 1)\n *Hello1* \n#else\n *Hello2* \n#end\n",
                    "Hello2", false, getXWikiContext());
        }


        public void testInclude(String text, String result) throws XWikiException {
            XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
            XWikiStoreInterface store = getXWiki().getStore();

            XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
            doc1.setContent("This is the topic name: $doc.name");
            doc1.setAuthor("FirstAuthor");
            doc1.setParent(Utils.parent);
            store.saveXWikiDoc(doc1, getXWikiContext());

            XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
            doc2.setAuthor("SecondAuthor");
            doc2.setContent(text);
            RenderTest.renderTest(wikiengine, doc2, result, false, getXWikiContext());
        }

        public void testIncludeTopic() throws XWikiException {
            testInclude("#includeTopic(\"Test.WebHome\")", "This is the topic name");
            testInclude("#includeTopic(\"Test.WebHome\")", "WebHome");
        }

        public void testIncludeForm() throws XWikiException {
            testInclude( "#includeForm(\"Test.WebHome\")", "IncludeTest");
        }


        public void testIncludeTopicContext() throws XWikiException {
           testInclude("#includeTopic(\"Test.WebHome\")\n$doc.author", "SecondAuthor");
        }

        public void testIncludeFormContext() throws XWikiException {
            testInclude( "#includeForm(\"Test.WebHome\")\n$doc.author", "SecondAuthor");
        }

        public void testIncludeFromOtherDatabase() throws XWikiException, HibernateException {
          getXWikiContext().setDatabase("xwikitest2");
          StoreHibernateTest.cleanUp(getXWiki().getHibernateStore(), true, true, getXWikiContext());
          String content = Utils.content1;
          Utils.content1 = "XWiki Users";
          Utils.createDoc(getXWiki().getHibernateStore(),"XWiki", "XWikiUsers", getXWikiContext());
          Utils.content1 = content;
          getXWikiContext().setDatabase("xwikitest");

          testInclude( "#includeTopic(\"xwikitest2:XWiki.XWikiUsers\")", "XWiki Users");
        }

        public void testIncludeFromOtherDatabaseContext() throws XWikiException, HibernateException {
          testIncludeFromOtherDatabase();
          testInclude( "#includeTopic(\"xwikitest2:XWiki.XWikiUsers\")\n$doc.author", "SecondAuthor");
        }

    public void testVelocityError() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(getXWiki(), getXWikiContext());
        RenderTest.renderTest(wikiengine, "#skype(hello)",
                "hello", false, getXWikiContext());
    }

    public void testIncludeMacro() throws Exception {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        XWikiStoreInterface store = getXWiki().getHibernateStore();

        XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
        doc1.setContent("#macro(hello)\ncoucou\n#end");
        doc1.setAuthor("FirstAuthor");
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, getXWikiContext());

        XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
        doc2.setAuthor("SecondAuthor");
        doc2.setContent("#includeMacros(\"Test.WebHome\")\n#hello()");
        store.saveXWikiDoc(doc2, getXWikiContext());
        RenderTest.renderTest(wikiengine, doc2, "coucou", false, getXWikiContext());
    }

 }
