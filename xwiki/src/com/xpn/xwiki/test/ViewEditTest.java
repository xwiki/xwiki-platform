
package com.xpn.xwiki.test;

import org.apache.cactus.WebRequest;
import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebResponse;
import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletException;
import java.io.IOException;

import net.sf.hibernate.HibernateException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.XWikiException;

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

 * Created by
 * User: Ludovic Dubost
 * Date: 11 janv. 2004
 * Time: 12:08:01
 */

public class ViewEditTest extends ServletTestCase {

    public static String hibpath = "hibernate-test.cfg.xml";

    public void setUp() {};
    public void cleanUp() {};

    public void clientSetUp() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
        XWikiSimpleDoc doc1 = new XWikiSimpleDoc(com.xpn.xwiki.test.StoreTest.web, "ViewOkTest");
        doc1.setContent(com.xpn.xwiki.test.StoreTest.content1);
        doc1.setAuthor(com.xpn.xwiki.test.StoreTest.author);
        doc1.setParent(com.xpn.xwiki.test.StoreTest.parent);
        hibstore.saveXWikiDoc(doc1);
    }

    public void beginViewNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        webRequest.setURL("127.0.0.1:9080", "/xwiki", "/testbin", "/view/" + com.xpn.xwiki.test.StoreTest.web + "/"
                            + "ViewNotOkTest", "");
    }

    public void endViewNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Page should have generated an error", result.indexOf("No row")!=-1);
    }

    public void testViewNotOk() throws IOException, ServletException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
    }

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        clientSetUp();
        webRequest.setURL("127.0.0.1:9080", "/xwiki" , "/testbin", "/view/" + com.xpn.xwiki.test.StoreTest.web + "/"
                            + "ViewOkTest", "");
    }

    public void endViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewOk() throws IOException, ServletException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
    }

}
