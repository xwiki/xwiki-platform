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
 * Date: 6 mars 2004
 * Time: 12:01:47
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import net.sf.hibernate.HibernateException;
import org.apache.velocity.app.Velocity;

import java.util.HashMap;


public class XWikiTest extends TestCase {

    private XWiki xwiki;
     private XWikiContext context;

     public XWikiHibernateStore getHibStore() {
         XWikiStoreInterface store = xwiki.getStore();
         if (store instanceof XWikiCacheInterface)
             return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
         else
             return (XWikiHibernateStore) store;
     }

     public XWikiStoreInterface getStore() {
         return xwiki.getStore();
     }

     public void setUp() throws Exception {
         context = new XWikiContext();
         xwiki = new XWiki("./xwiki.cfg", context);
         context.setWiki(xwiki);
         Velocity.init("velocity.properties");
         StoreHibernateTest.cleanUp(getHibStore(), context);
     }

     public void tearDown() throws HibernateException {
         getHibStore().shutdownHibernate(context);
         xwiki = null;
         context = null;
         System.gc();
     }

    public void testDefaultSkin() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        RenderTest.renderTest(wikiengine, "$xwiki.getSkin()",
                "default", true, context);
    }

    public void testModifiedSkin() throws XWikiException {
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", context);
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        RenderTest.renderTest(wikiengine, "$xwiki.getSkin()",
                "altern", true, context);
    }

    public void testPassword() throws XWikiException, HibernateException {
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", context);
        XWikiDocInterface doc = xwiki.getDocument("XWiki.LudovicDubost", context);
        String xml = doc.getXMLContent(context);
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should contain password", xml.indexOf("toto")!=-1);
        Document ddoc = new Document(doc, context);
        xml = ddoc.getXMLContent();
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should not contain password", xml.indexOf("toto")==-1);
    }

}
