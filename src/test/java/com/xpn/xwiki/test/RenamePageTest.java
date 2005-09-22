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

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.hibernate.cfg.Configuration;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class RenamePageTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public void testBasicRenamePage() throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB] [Test.BacklinksInput]");
        List expected_list1 = new ArrayList();
        expected_list1.add("Test.AAA");
        expected_list1.add("Test.BBBB");
        expected_list1.add("Test.BacklinksInput");
        getXWiki().getStore().saveXWikiDoc(doc1, getXWikiContext());

        long docId = doc1.getId();
        String sql =  ("select xwl_link from xwikilinks where xwl_doc_id = "+docId);
        getXWiki().getHibernateStore().beginTransaction(getXWikiContext());
        List result = runSQLwithReturn(getXWiki().getHibernateStore(), sql , getXWikiContext());
        assertTrue("SQL return is not the same size", result.size() ==  expected_list1.size());

        List loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument("Test.BacklinksTarget", getXWikiContext()).getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks.size());
        for (int i=0;i<loadedLinks.size();i++) {
            String item = ((XWikiLink)loadedLinks.get(i)).getLink();
            boolean ok = expected_list1.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        XWikiDocument newdoc = getXWiki().renamePage(doc1, getXWikiContext(), "Test.NewName");

        assertTrue("Renaming is wrong", newdoc.getFullName().equals("Test.NewName"));
        assertTrue("New doc doesn't exists", getXWiki().getStore().exists(getXWiki().getDocument("Test.NewName", getXWikiContext()), getXWikiContext()) );
        assertTrue("Old doc still exists", !getXWiki().getStore().exists(doc1, getXWikiContext()) );
        assertTrue("Old doc still exists", !getXWiki().getStore().exists(getXWiki().getDocument("Test.BackLinksTarget", getXWikiContext()), getXWikiContext()) );

        loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument("Test.BacklinksTarget", getXWikiContext()).getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks.size());

        loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument("Test.NewName", getXWikiContext()).getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks.size());
        for (int i=0;i<loadedLinks.size();i++) {
            String item = ((XWikiLink)loadedLinks.get(i)).getLink();
            boolean ok = expected_list1.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testObjectsRenamedDoc() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "BacklinksTarget");
        doc.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB] [Test.BacklinksInput]");

        List attachmentList = new ArrayList();

        Object object0 = new Object();
        Object object1 = new Object();
        Object object2 = new Object();
        Object object3 = new Object();
        Object object4 = new Object();
        attachmentList.add(0, object0);
        attachmentList.add(1, object1);
        attachmentList.add(2, object2);
  //      attachmentList.set(1, Utils.prepareObject(doc));
  //      attachmentList.set(2, Utils.prepareObject(doc));
        doc.setAttachmentList(attachmentList);
        getXWiki().getStore().saveXWikiDoc(doc, getXWikiContext());

        XWikiDocument newdoc = getXWiki().renamePage(doc, getXWikiContext(), "Test.NewName");

        assertTrue("New doc doesn't exists", getXWiki().getStore().exists(getXWiki().getDocument("Test.NewName", getXWikiContext()), getXWikiContext()) );
        assertTrue("Old doc still exists", !getXWiki().getStore().exists(getXWiki().getDocument("Test.BackLinksTarget", getXWikiContext()), getXWikiContext()) );
        assertTrue("Attachment List size is wrong", newdoc.getAttachmentList().size() == attachmentList.size());
    }
}
