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

import com.xpn.xwiki.plugin.packaging.Package;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiException;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;


public class PackageTest extends HibernateTestCase {


    public void setUp() throws Exception {
        super.setUp();
        prepareData();
    }

    public void prepareData() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "first");
        doc.setContent("blop, first test page");
        getXWiki().saveDocument(doc, getXWikiContext());

        doc = new XWikiDocument("Test", "second");
        doc.setContent("blop, second test page");
        getXWiki().saveDocument(doc, getXWikiContext());

        doc = new XWikiDocument("Test", "third");
        doc.setContent("blop, third test page");
        getXWiki().saveDocument(doc, getXWikiContext());

        getXWikiContext().put("vcontext", new VelocityContext());
    }

    public void testExportWiki() throws IOException, XWikiException {
        Package myPackage = new Package();
        myPackage.setWithVersions(false);
        myPackage.addAllWikiDocuments(getXWikiContext());
        assertEquals(3, myPackage.getFiles().size());
        testDocName("Test.first", myPackage.getFiles());
        testDocName("Test.second", myPackage.getFiles());
        testDocName("Test.third", myPackage.getFiles());
        assertEquals(myPackage.isBackupPack(), true);
    }


    public void testPackageConfig() throws XWikiException, IOException {
        Package myPackage = new Package();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        myPackage.setName("Package Test");
        myPackage.setDescription("Package Description");
        myPackage.setLicence("GPL");
        myPackage.setAuthorName("Jeremi Joslin");
        myPackage.add("Test.first", getXWikiContext());
        myPackage.add("Test.third", getXWikiContext());
        myPackage.export(os, getXWikiContext());

        Package myOtherPackage = new Package();
        myOtherPackage.Import(os.toByteArray(), getXWikiContext());
        assertEquals(myOtherPackage.getName(), myPackage.getName());
        assertEquals(myOtherPackage.getDescription(), myPackage.getDescription());
        assertEquals(myOtherPackage.getLicence(), myPackage.getLicence());
        assertEquals(myOtherPackage.getAuthorName(), myPackage.getAuthorName());
        testDocName("Test.first", myOtherPackage.getFiles());
        testDocName("Test.third", myOtherPackage.getFiles());
        assertEquals(myOtherPackage.isBackupPack(), false);

        testImportPackage(myOtherPackage);

        myOtherPackage = new Package();
        myOtherPackage.Import(os.toByteArray(), getXWikiContext());
        testOverwrite(myOtherPackage);
    }

    public void testImportPackage(Package pack) throws XWikiException {
        // Setup database xwikitest2
        getXWikiContext().setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(getXWikiContext().getWiki().getHibernateStore(), getXWikiContext());

        XWikiDocument doc = getXWikiContext().getWiki().getDocument("Test.first", getXWikiContext());
        assertTrue(doc.isNew());

        pack.install(getXWikiContext());

        doc = getXWikiContext().getWiki().getDocument("Test.first", getXWikiContext());
        assertFalse(doc.isNew());

        doc = getXWikiContext().getWiki().getDocument("Test.third", getXWikiContext());
        assertFalse(doc.isNew());

    }

    public void testOverwrite(Package pack) throws XWikiException {
        XWikiDocument doc = getXWikiContext().getWiki().getDocument("Test.third", getXWikiContext());
        doc.setContent("test overwrite");
        getXWiki().saveDocument(doc, getXWikiContext());

        int ret = pack.testInstall(getXWikiContext());
        assertEquals(ret, DocumentInfo.INSTALL_ALREADY_EXIST);
        for (int i = 0; i < pack.getFiles().size(); i++)
            assertEquals(((DocumentInfo)pack.getFiles().get(i)).testInstall(getXWikiContext()), DocumentInfo.INSTALL_ALREADY_EXIST);
        ret = pack.install(getXWikiContext());
        assertEquals(ret, DocumentInfo.INSTALL_OK);

        doc = getXWikiContext().getWiki().getDocument("Test.third", getXWikiContext());
        assertEquals("test overwrite", doc.getContent());


        for (int i = 0; i < pack.getFiles().size(); i++)
            ((DocumentInfo)pack.getFiles().get(i)).setAction(DocumentInfo.ACTION_OVERWRITE);
        ret = pack.install(getXWikiContext());
        assertEquals(ret, DocumentInfo.INSTALL_OK);

        doc = getXWikiContext().getWiki().getDocument("Test.third", getXWikiContext());
        assertEquals("blop, third test page", doc.getContent());

    }

    public void testDocName(String fileName, List files)
    {
        for (int i = 0; i < files.size(); i++)
        {
            DocumentInfo doc = (DocumentInfo) files.get(i);
            if (doc.getFullName().compareTo(fileName) == 0)
                return;
        }
        assertTrue("Document not found: " + fileName, false);
    }



    public void testPackageAPI()
    {

    }



}
