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
import com.xpn.xwiki.render.groovy.XWikiGroovyRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;

public class GroovyRenderTest extends HibernateTestCase {

    public void testBasics() throws XWikiException {
        XWikiRenderer wikibase = new XWikiGroovyRenderer();
        getXWiki().setRightService(new GroovyTestRightService());
        RenderTest.renderTest(wikibase, "<% foo = \"Groovy\"\nprintln \"Hello $foo World!\" %>",
                "Hello Groovy World!", false, getXWikiContext());
        RenderTest.renderTest(wikibase, "<% count = 0\nif ( count == 1)\n{ println \"A${count}A\" }\nelse\n { println \"B${count}B\" }\n %>",
                "B0B", false, getXWikiContext());
    }

    public void testWithFunction() throws XWikiException {
        XWikiRenderer wikibase = new XWikiGroovyRenderer();
        getXWiki().setRightService(new GroovyTestRightService());
        RenderTest.renderTest(wikibase, "<%  def add(int a, int b) { return a+b }\n println add(1,2)\n %>",
                "3", false, getXWikiContext());
    }

    public void testWithInclude() throws Exception {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        XWikiStoreInterface store = getXWiki().getStore();
        getXWiki().setRightService(new GroovyTestRightService());

        XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
        doc1.setContent("<%  testvar = \"${doc.name}\" %>");
        doc1.setAuthor("FirstAuthor");
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, getXWikiContext());

        XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
        doc2.setAuthor("SecondAuthor");
        doc2.setContent("#includeMacros(\"Test.WebHome\")<% println testvar %>");
        store.saveXWikiDoc(doc2, getXWikiContext());
        RenderTest.renderTest(wikiengine, doc2, "IncludeTest", false, getXWikiContext());
    }

    public void testWithFunctionInclude() throws Exception {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        XWikiStoreInterface store = getXWiki().getStore();
        getXWiki().setRightService(new GroovyTestRightService());

        XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
        doc1.setContent("<%  def add(int a, int b) { return a+b } %>");
        doc1.setAuthor("FirstAuthor");
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, getXWikiContext());

        XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
        doc2.setAuthor("SecondAuthor");
        doc2.setContent("#includeMacros(\"Test.WebHome\")<% println add(1,2) %>");
        store.saveXWikiDoc(doc2, getXWikiContext());
        RenderTest.renderTest(wikiengine, doc2, "3", false, getXWikiContext());
    }

 }
