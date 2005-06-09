/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import java.util.List;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.i18n.i18n;

public class i18nTest extends HibernateTestCase {

     public void testTranslation() throws XWikiException {
         Utils.createDoc(getXWiki().getHibernateStore(), "Test", "TranslationTest", getXWikiContext());
         XWikiDocument doc = getXWiki().getDocument("Test.TranslationTest", getXWikiContext());
         doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
         XWikiDocument doc2 = new XWikiDocument("Test", "TranslationTest");
         doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
         doc2.setLanguage(i18n.LANGUAGE_FRENCH);
         doc2.setTranslation(i18n.TRANSLATION_CONTENT);
         getXWiki().saveDocument(doc2, getXWikiContext());
         String tcontent = doc.getTranslatedContent(i18n.LANGUAGE_FRENCH, getXWikiContext());
         assertEquals("Translated content is not correct", "Bonjour 1. Bonjour 2. Bonjour 3", tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_ENGLISH, getXWikiContext());
         assertEquals("Translated content is not correct", Utils.content1, tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_GERMAN, getXWikiContext());
         assertEquals("Translated content is not correct", Utils.content1, tcontent);
         tcontent = doc.getTranslatedContent(i18n.LANGUAGE_DEFAULT, getXWikiContext());
         assertEquals("Translated content is not correct", Utils.content1, tcontent);

         List translations = doc.getTranslationList(getXWikiContext());
         assertEquals("There should be 1 translations", 1, translations.size());
         assertEquals("First translation should be english", "fr", translations.get(0));
     }
}
