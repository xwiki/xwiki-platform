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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.groovy.XWikiGroovyRenderer;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import org.hibernate.HibernateException;

public class GroovyRenderTest extends TestCase {

    private XWiki xwiki;
    private XWikiContext context;

    // TODO: Refactor this portion of code. It seems it is used in lots of places and it would 
    // be good to have it in a common location (for example in a XWikiTestSetup class)
    public void setUp() throws Exception {

        XWikiConfig config = new XWikiConfig();
        // TODO: Should be modified to use a memory store for testing or a mock store
        config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
        config.put("xwiki.store.hibernate.path", getClass().getResource(StoreHibernateTest.HIB_LOCATION).getFile());

        context = new XWikiContext();
        xwiki = new XWiki(config, context);
        context.setWiki(xwiki);
        StoreHibernateTest.cleanUp(xwiki.getHibernateStore(), context);
    }

    public void tearDown() throws HibernateException {
        xwiki.getHibernateStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void testBasics() throws XWikiException {
        XWikiRenderer wikibase = new XWikiGroovyRenderer();
        xwiki.setRightService(new GroovyTestRightService());
        RenderTest.renderTest(wikibase, "<% foo = \"Groovy\"\nprintln \"Hello $foo World!\" %>",
                "Hello Groovy World!", false, context);
        RenderTest.renderTest(wikibase, "<% count = 0\nif ( count == 1)\n{ println \"A${count}A\" }\nelse\n { println \"B${count}B\" }\n %>",
                "B0B", false, context);
    }

    public void testWithFunction() throws XWikiException {
        XWikiRenderer wikibase = new XWikiGroovyRenderer();
        xwiki.setRightService(new GroovyTestRightService());
        RenderTest.renderTest(wikibase, "<%  def add(int a, int b) { return a+b }\n println add(1,2)\n %>",
                "3", false, context);
    }

    public void testWithInclude() throws Exception {
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();
        XWikiStoreInterface store = xwiki.getStore();
        xwiki.setRightService(new GroovyTestRightService());

        XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
        doc1.setContent("<%  testvar = \"${doc.name}\" %>");
        doc1.setAuthor("FirstAuthor");
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, context);

        XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
        doc2.setAuthor("SecondAuthor");
        doc2.setContent("#includeMacros(\"Test.WebHome\")<% println testvar %>");
        store.saveXWikiDoc(doc2, context);
        RenderTest.renderTest(wikiengine, doc2, "IncludeTest", false, context);
    }

    public void testWithFunctionInclude() throws Exception {
        XWikiRenderingEngine wikiengine = xwiki.getRenderingEngine();
        XWikiStoreInterface store = xwiki.getStore();
        xwiki.setRightService(new GroovyTestRightService());

        XWikiDocument doc1 = new XWikiDocument("Test", "WebHome");
        doc1.setContent("<%  def add(int a, int b) { return a+b } %>");
        doc1.setAuthor("FirstAuthor");
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, context);

        XWikiDocument doc2 = new XWikiDocument("Other", "IncludeTest");
        doc2.setAuthor("SecondAuthor");
        doc2.setContent("#includeMacros(\"Test.WebHome\")<% println add(1,2) %>");
        store.saveXWikiDoc(doc2, context);
        RenderTest.renderTest(wikiengine, doc2, "3", false, context);
    }

 }
