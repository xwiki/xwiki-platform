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
 * User: ludovic
 * Date: 14 avr. 2004
 * Time: 18:27:52
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.i18n.i18n;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheInterface;
import org.apache.velocity.app.Velocity;
import net.sf.hibernate.HibernateException;
import junit.framework.TestCase;

import java.util.List;

public class i18nTest extends TestCase {

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


     public void testTranslation() throws XWikiException {
         Utils.createDoc(getHibStore(), "Test", "TranslationTest", context);
         XWikiDocInterface doc = xwiki.getDocument("Test.TranslationTest", context);
         doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
         XWikiDocInterface doc2 = new XWikiSimpleDoc("Test", "TranslationTest");
         doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
         doc2.setLanguage(i18n.LANGUAGE_FRENCH);
         doc2.setTranslation(i18n.TRANSLATION_CONTENT);
         xwiki.saveDocument(doc2, context);
         String tcontent = doc.getTranslatedContent(i18n.LANGUAGE_FRENCH, context);
         assertEquals("Translated content is not correct", "Bonjour 1. Bonjour 2. Bonjour 3", tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_ENGLISH, context);
         assertEquals("Translated content is not correct", Utils.content1, tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_GERMAN, context);
         assertEquals("Translated content is not correct", Utils.content1, tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_DEFAULT, context);
         assertEquals("Translated content is not correct", Utils.content1, tcontent);

         List translations = doc.getTranslationList(context);
         assertEquals("There should be 1 translations", 1, translations.size());
         assertEquals("First translation should be english", "fr", translations.get(0));
     }
}
