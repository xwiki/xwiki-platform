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


public class StoreTest extends TestCase {

    String name = "WebHome";
    String name2 = "Globals";
    String web = "Main";
    String content1 = "Hello 1\nHello 2\nHello 3\n";
    String content3 = "Hello 1\nIntermediary line\nHello 2\nHello 3\n";
    String author = "VictorHugo";
    String author2 = "JulesVerne";
    String parent = "Main.WebHome";
    String version = "1.1";
    String version2 = "1.2";

    String rcspath = "./rcs";
    String hibpath = "/hibernate-test.cfg.xml";

    public void clean() {
        File file = new File(rcspath + "/" + web + "/" + name + ".txt");
        file.delete();
        file = new File(rcspath + "/" + web + "/" + name + ".txt,v");
        file.delete();
        file = new File(rcspath + "/" + web + "/" + name2 + ".txt");
        file.delete();
        file = new File(rcspath + "/" + web + "/" + name2 + ".txt,v");
        file.delete();
    }

    public void setUp() {
        clean();
    }

    public void tearDown() {
       // clean();
    }

    public void testStandardReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException {
        XWikiSimpleDoc doc1 = new XWikiSimpleDoc(web, name);
        doc1.setContent(content1);
        doc1.setAuthor(author);
        doc1.setParent(parent);
        store.saveXWikiDoc(doc1);
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc(web, name);
        store.loadXWikiDoc(doc2);
        String content2 = doc2.getContent();
        assertEquals(content1,content2);
        assertEquals(doc2.getVersion(), version);
        assertEquals(doc2.getParent(), parent);
        assertEquals(doc2.getAuthor(), author);
        doc2.setContent(content3);
        doc2.setAuthor(author2);
        store.saveXWikiDoc(doc2);
        XWikiSimpleDoc doc3 = new XWikiSimpleDoc(web, name);
        store.loadXWikiDoc(doc3);
        String content3b = doc3.getContent();
        assertEquals(content3,content3b);
        assertEquals(doc3.getAuthor(), author2);
        assertEquals(doc3.getVersion(), version2);
    }

    public void setStandardData() {
        name = "WebHome";
        name2 = "Globals";
        web = "Main";
        content1 = "Hello 1\nHello 2\nHello 3\n";
        content3 = "Hello 1\nIntermediary line\nHello 2\nHello 3\n";
        author = "VirtorHugo";
        author2 = "JulesVerne";
        parent = "Main.WebHome";
        version = "1.1";
        version2 = "1.2";
    }

    public void setMediumData() {

        setStandardData();

        while (content1.length()<1000)
                content1 += content1;


        while (author.length()<120)
                author += author;
        while (content3.length()<1000)
                content3 += content3;
    }

    public String getData(File file) throws IOException {
        StringBuffer content = new StringBuffer();
        BufferedReader fr = new BufferedReader(new FileReader(file));
        String line;
        while ((line = fr.readLine())!=null) {
            content.append(line);
            content.append("\n");
        }
        fr.close();
        return content.toString();
    }

    public void setBigData() throws IOException {
        setStandardData();
        while (author.length()<120)
                author += author;

        File file1 = new File(rcspath + "/" + web + "/" + name2 + ".txt.1");
        File file3 = new File(rcspath + "/" + web + "/" + name2 + ".txt.2");
        content1 = getData(file1);
        content3 = getData(file3);
    }


    public void testVersionedReadWrite(XWikiStoreInterface store,String web, String name) throws XWikiException {
        XWikiSimpleDoc doc4 = new XWikiSimpleDoc(web, name);
        store.loadXWikiDoc(doc4,version);
        String content4 = doc4.getContent();
        assertEquals(content1,content4);
        assertEquals(doc4.getVersion(),version);
        assertEquals(doc4.getAuthor(), author);
        Version[] versions = store.getXWikiDocVersions(doc4);
        assertTrue(versions.length==2);
    }



    public void testRCSStandardReadWrite() throws XWikiException {
        setStandardData();
        XWikiStoreInterface rcsstore = new XWikiRCSFileStore(rcspath);
        testStandardReadWrite(rcsstore, web, name);
    }

    public void testHibernateStandardReadWrite() throws XWikiException {
        setStandardData();
        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        testStandardReadWrite(hibstore, web, name);
    }

    public void testRCSVersionedReadWrite() throws XWikiException {
        setStandardData();
        XWikiStoreInterface rcsstore = new XWikiRCSFileStore(rcspath);
        testStandardReadWrite(rcsstore, web, name);
        testVersionedReadWrite(rcsstore, web, name);
    }

    public void testHibernateVersionedReadWrite() throws XWikiException {
        setStandardData();
        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        testStandardReadWrite(hibstore, web, name);
        testVersionedReadWrite(hibstore, web, name);
    }

    public void testRCSMediumReadWrite() throws XWikiException {
        setMediumData();
        XWikiStoreInterface rcsstore = new XWikiRCSFileStore(rcspath);
        testStandardReadWrite(rcsstore, web, name);
        testVersionedReadWrite(rcsstore, web, name);
    }

    public void testHibernateMediumReadWrite() throws XWikiException {
        setMediumData();
        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        testStandardReadWrite(hibstore, web, name);
        testVersionedReadWrite(hibstore, web, name);
    }

    public void testRCSBigVersionedReadWrite() throws XWikiException, IOException {
        setBigData();
        XWikiStoreInterface rcsstore = new XWikiRCSFileStore(rcspath);
        testStandardReadWrite(rcsstore, web, name2);
        testVersionedReadWrite(rcsstore, web, name2);
    }

    public void testHibernateBigVersionedReadWrite() throws XWikiException, IOException {
        setBigData();
        XWikiStoreInterface hibstore = new XWikiHibernateStore(hibpath);
        testStandardReadWrite(hibstore, web, name2);
        testVersionedReadWrite(hibstore, web, name2);
    }

}
