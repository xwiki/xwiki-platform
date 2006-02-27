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
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import java.io.File;

import junit.framework.TestCase;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiRCSFileStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

public class StoreRCSFileTest extends TestCase {

    public void cleanUp() {
        File file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name + ".txt,v");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt");
        file.delete();
        file = new File(Utils.rcspath + "/" + Utils.web + "/" + Utils.name2 + ".txt,v");
        file.delete();
    }

    public void setUp() {
        cleanUp();
    }

    public XWikiStoreInterface getStore() {
        XWikiStoreInterface store = new XWikiRCSFileStore(Utils.rcspath, Utils.rcsattachmentpath);
        return store;
    }

    public void testMetaData() {
        XWikiDocument doc = new XWikiDocument();
        doc.setContent("Hello1\r\nHello2\r\r\nHello3");
        doc.setAuthor("LudovicDubost");
        doc.setParent("Test.WebHome");
        String str = ((XWikiRCSFileStore)getStore()).getMetaFullContent(doc);
        assertTrue("Result should contain Hello", str.indexOf("Hello")!=-1);
        assertTrue("Result should contain Author", str.indexOf("author=\"LudovicDubost\"")!=-1);
        assertTrue("Result should contain parent", str.indexOf("%META:TOPICPARENT{name=\"Test.WebHome\" }")!=-1);
    }

}
