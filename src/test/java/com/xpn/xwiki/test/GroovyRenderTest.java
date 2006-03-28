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
 * @author vmassol
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
        AbstractRenderTest.renderTest(wikibase, "<% foo = \"Groovy\"\nprintln \"Hello $foo World!\" %>",
                "Hello Groovy World!", false, getXWikiContext());
        AbstractRenderTest.renderTest(wikibase, "<% count = 0\nif ( count == 1)\n{ println \"A${count}A\" }\nelse\n { println \"B${count}B\" }\n %>",
                "B0B", false, getXWikiContext());
    }

    public void testWithFunction() throws XWikiException {
        XWikiRenderer wikibase = new XWikiGroovyRenderer();
        getXWiki().setRightService(new GroovyTestRightService());
        AbstractRenderTest.renderTest(wikibase, "<%  def add(int a, int b) { return a+b }\n println add(1,2)\n %>",
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
        AbstractRenderTest.renderTest(wikiengine, doc2, "IncludeTest", false, getXWikiContext());
    }

    /*
     TODO: This fail is known to test
     */
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
        AbstractRenderTest.renderTest(wikiengine, doc2, "3", false, getXWikiContext());
    }

 }
