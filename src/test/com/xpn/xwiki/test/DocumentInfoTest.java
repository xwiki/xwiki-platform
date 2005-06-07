/**
 * ===================================================================
 *
 * Copyright (c) 2005 Jérémi Joslin, XpertNet, All rights reserved.
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

import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiException;
import org.apache.velocity.VelocityContext;


public class DocumentInfoTest extends HibernateTestCase {

    public void prepareData() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "first");

        doc.setContent("blop, first test page");
        getXWiki().saveDocument(doc, getXWikiContext());
        getXWikiContext().put("vcontext", new VelocityContext());
    }

    public void testActionTo() {
        assertEquals(DocumentInfo.actionToString(DocumentInfo.ACTION_MERGE), "merge");
        assertEquals(DocumentInfo.actionToString(DocumentInfo.ACTION_OVERWRITE), "overwrite");
        assertEquals(DocumentInfo.actionToString(DocumentInfo.ACTION_SKIP), "skip");
        assertEquals(DocumentInfo.actionToString(DocumentInfo.ACTION_NOT_DEFINED), "Not defined");

        assertEquals(DocumentInfo.actionToInt("merge"), DocumentInfo.ACTION_MERGE);
        assertEquals(DocumentInfo.actionToInt("overwrite"), DocumentInfo.ACTION_OVERWRITE);
        assertEquals(DocumentInfo.actionToInt("skip"), DocumentInfo.ACTION_SKIP);
        assertEquals(DocumentInfo.actionToInt("Not defined"), DocumentInfo.ACTION_NOT_DEFINED);

    }

    public void testInstallStatusToString() {
        assertEquals(DocumentInfo.installStatusToString(DocumentInfo.INSTALL_IMPOSSIBLE), "Impossible");
        assertEquals(DocumentInfo.installStatusToString(DocumentInfo.INSTALL_OK), "Ok");
        assertEquals(DocumentInfo.installStatusToString(DocumentInfo.INSTALL_ERROR), "Error");
        assertEquals(DocumentInfo.installStatusToString(DocumentInfo.INSTALL_ALREADY_EXIST), "Already exist");
    }


    public void testTestInstall() throws XWikiException {
        prepareData();

        XWikiDocument doc = new XWikiDocument("Test", "first");
        doc.setContent("overwrite");
        DocumentInfo docInfo = new DocumentInfo(doc);

        assertEquals("install must overwrite", docInfo.testInstall(getXWikiContext()), DocumentInfo.INSTALL_ALREADY_EXIST);

        docInfo.setAction(DocumentInfo.ACTION_SKIP);
        assertEquals("install must return Ok", docInfo.install(getXWikiContext()), DocumentInfo.INSTALL_OK);

        assertEquals("document must be new (not saved)", doc.isNew(), true);

        docInfo.setAction(DocumentInfo.ACTION_OVERWRITE);
        assertEquals("install must return Ok", docInfo.install(getXWikiContext()), DocumentInfo.INSTALL_OK);

        assertEquals("document must not be new (saved)", doc.isNew(), false);

        XWikiDocument doctest = getXWikiContext().getWiki().getDocument(doc.getFullName(), getXWikiContext());

        assertEquals("document content must be equals", doc.getContent(), doctest.getContent());
    }

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(getXWiki(), getXWikiContext(), fullname, user, group, level, allow, global);
    }

    public void testSecurity() throws XWikiException {
        updateRight("Test.first", "XWiki.LudovicDubost", "", "edit", false, false);

        getXWikiContext().put("vcontext", new VelocityContext());

        String userSave = getXWikiContext().getUser();

        getXWikiContext().setUser("XWiki.LudovicDubost");

        XWikiDocument doc = new XWikiDocument("Test", "first");
        doc.setContent("overwrite impossible");
        DocumentInfo docInfo = new DocumentInfo(doc);

        assertEquals("testinstall must say impossible", docInfo.testInstall(getXWikiContext()), DocumentInfo.INSTALL_IMPOSSIBLE);

        assertEquals("install must say impossible", docInfo.install(getXWikiContext()), DocumentInfo.INSTALL_IMPOSSIBLE);

        XWikiDocument doctest = getXWikiContext().getWiki().getDocument(doc.getFullName(), getXWikiContext());
        assertTrue("document content must be not equals", doc.getContent().compareTo(doctest.getContent()) != 0);

        getXWikiContext().setUser(userSave);
    }

}
