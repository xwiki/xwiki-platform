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
 * Created by
 * User: Ludovic Dubost
 * Date: 24 nov. 2003
 * Time: 10:55:50
 */
package com.xpn.xwiki.test;

import junit.framework.*;
import com.xpn.xwiki.store.*;
import com.xpn.xwiki.doc.*;
import com.xpn.xwiki.XWikiException;
import java.io.*;
import org.apache.commons.jrcs.rcs.Version;


public abstract class StoreTest extends TestCase {


    public abstract XWikiStoreInterface getStore();

    public void testStandardReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException {
        XWikiSimpleDoc doc1 = new XWikiSimpleDoc(web, name);
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1);
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc(web, name);
        doc2 = (XWikiSimpleDoc) store.loadXWikiDoc(doc2);
        String content2 = doc2.getContent();
        assertEquals(Utils.content1,content2);
        assertEquals(doc2.getVersion(), Utils.version);
        assertEquals(doc2.getParent(), Utils.parent);
        assertEquals(doc2.getAuthor(), Utils.author);
        doc2.setContent(Utils.content3);
        doc2.setAuthor(Utils.author2);
        store.saveXWikiDoc(doc2);
        XWikiSimpleDoc doc3 = new XWikiSimpleDoc(web, name);
        doc3 = (XWikiSimpleDoc) store.loadXWikiDoc(doc3);
        String content3b = doc3.getContent();
        assertEquals(Utils.content3,content3b);
        assertEquals(doc3.getAuthor(), Utils.author2);
        assertEquals(doc3.getVersion(), Utils.version2);
    }



    public void testVersionedReadWrite(XWikiStoreInterface store,String web, String name) throws XWikiException {
        XWikiSimpleDoc doc3 = new XWikiSimpleDoc(web, name);
        doc3 = (XWikiSimpleDoc) store.loadXWikiDoc(doc3);
        XWikiDocInterface doc4 = store.loadXWikiDoc(doc3,Utils.version);
        String content4 = doc4.getContent();
        assertEquals(Utils.content1,content4);
        assertEquals(doc4.getVersion(),Utils.version);
        assertEquals(doc4.getAuthor(), Utils.author);
        Version[] versions = store.getXWikiDocVersions(doc4);
        assertTrue(versions.length==2);
    }



    public void testStandardReadWrite() throws XWikiException {
        Utils.setStandardData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name);
    }

    public void testVersionedReadWrite() throws XWikiException {
        Utils.setStandardData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name);
        testVersionedReadWrite(store, Utils.web, Utils.name);
    }

    public void testMediumReadWrite() throws XWikiException {
        Utils.setMediumData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name);
        testVersionedReadWrite(store, Utils.web, Utils.name);
    }

    public void testBigVersionedReadWrite() throws XWikiException, IOException {
        Utils.setBigData();
        XWikiStoreInterface store = getStore();
        testStandardReadWrite(store, Utils.web, Utils.name2);
        testVersionedReadWrite(store, Utils.web, Utils.name2);
    }

}
