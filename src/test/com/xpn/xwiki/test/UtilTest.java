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
 * Date: 24 nov. 2003
 * Time: 18:19:44
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;
import junit.framework.TestCase;
import org.hibernate.HibernateException;
import org.apache.oro.text.regex.MalformedPatternException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;


public class UtilTest extends TestCase {

    private XWiki xwiki;
    private XWikiContext context;

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void setUp() throws XWikiException {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);
        context.setDatabase("xwikitest");
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public void testTopicInfo() throws IOException {
        String topicinfo;
        Hashtable params;

        topicinfo = "author=\"ludovic\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        topicinfo = "author=\"ludovic\" date=\"1026671586\" format=\"1.0beta2\" version=\"1.1\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("ludovic"));
        assertTrue(params.get("date").equals("1026671586"));
        assertTrue(params.get("format").equals("1.0beta2"));
        assertTrue(params.get("version").equals("1.1"));
        topicinfo = "author=\"Ludovic Dubost\" format=\"1.0 beta\" version=\"1.2\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("author").equals("Ludovic Dubost"));
        assertTrue(params.get("format").equals("1.0 beta"));
        assertTrue(params.get("version").equals("1.2"));
        topicinfo = "test=\"%_Q_%Toto%_Q_%\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("\"Toto\""));
        topicinfo = "test=\"Ludovic%_N_%Dubost\"";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
        topicinfo = "   test=\"Ludovic%_N_%Dubost\"   ";
        params = Util.keyValueToHashtable(topicinfo);
        assertTrue(params.get("test").equals("Ludovic\nDubost"));
    }

    public void testgetDocumentFromPath() throws XWikiException {
        String path = "/view/Main/WebHome";
        XWikiDocument doc = xwiki.getDocumentFromPath(path, context);
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
         path = "/view/Main/WebHome/taratata.doc";
        doc = xwiki.getDocumentFromPath(path, context);
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
         path = "/view/Main/WebHome/blabla/tfdfdf.doc";
        doc = xwiki.getDocumentFromPath(path, context);
        assertEquals("Doc web is not correct", "Main", doc.getWeb());
        assertEquals("Doc name is not correct", "WebHome", doc.getName());
        path = "/view/Test/Titi/taratata.doc";
       doc = xwiki.getDocumentFromPath(path, context);
       assertEquals("Doc web is not correct", "Test", doc.getWeb());
       assertEquals("Doc name is not correct", "Titi", doc.getName());

    }

    public void testGetMatches() throws MalformedPatternException {
      String pattern = "#include(Topic|Form)\\(\"(.*?)\"\\)";
      List list = context.getUtil().getMatches("", pattern, 2);
      assertEquals("List should have not items", 0, list.size());
      list = context.getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi", pattern, 2);
      assertEquals("List should have one items", 1, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      list = context.getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi#includeForm(\"Main.Toto\")Hi", pattern, 2);
      assertEquals("List should have one items", 1, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      list = context.getUtil().getMatches("Hello#includeTopic(\"Main.Toto\")Hi#includeForm(\"XWiki.Tata\")Hi", pattern, 2);
      assertEquals("List should have two items", 2, list.size());
      assertEquals("List item 1 should be Main.Toto", "Main.Toto", list.get(0));
      assertEquals("List item 2 should be XWiki.Tata", "XWiki.Tata", list.get(1));
    }

    public void testSubstitute() {
      Util util = new Util();
      String result = util.substitute("hello", "Hello", "hello how are you. hello how are you");
      assertEquals("Wrong result", "Hello how are you. Hello how are you", result);

    }

    public void testServletURLFactory() throws MalformedURLException {
        XWikiContext context = new XWikiContext();
        URL url = new URL("http://www.xwiki.org/xwiki/bin/view/Main/WebHome");
        XWikiURLFactory factory = new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/");
        context.setURL(url);
        context.setURLFactory(factory);

        assertEquals("URL is incorrect",
                     new URL("http://www.xwiki.org/xwiki/bin/view/Main/Toto"),
                     factory.createURL("Main", "Toto", context));
        assertEquals("URL is incorrect",
                     new URL("http://www.xwiki.org/xwiki/bin/view/XWiki/Toto"),
                     factory.createURL("XWiki", "Toto", context));
        assertEquals("URL is incorrect",
                     new URL("http://www.xwiki.org/xwiki/bin/edit/XWiki/Toto"),
                     factory.createURL("XWiki", "Toto", "edit", context));
        assertEquals("URL is incorrect",
                     new URL("http://www.xwiki.org/xwiki/bin/edit/XWiki/Toto?raw=1"),
                     factory.createURL("XWiki", "Toto", "edit", "raw=1", null, context));
    }


}
