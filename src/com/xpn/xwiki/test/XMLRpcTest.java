/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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
 * Date: 19 janv. 2005
 * Time: 19:04:27
 */
package com.xpn.xwiki.test;

import org.apache.xmlrpc.XmlRpcClient;

import java.util.Vector;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.io.File;

import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import junit.framework.TestCase;

import javax.servlet.ServletContext;

public class XMLRpcTest  extends TestCase
    {
    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context = new XWikiContext();
    public XWiki xwiki;
    private XmlRpcClient rpcClient;
    private String loginToken;

    public XMLRpcTest ()
    {
        super();
    }

    public void setUp() throws Exception {
        super.setUp();
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);

        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "XmlRpcTest", context);

        XWikiDocument doc1 = xwiki.getDocument("Main.XmlRpcTest", context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);

        HashMap map = new HashMap();
        map.put("password", "admin");
        xwiki.createUser("Admin", map, "", "", "view, edit", context);

        rpcClient = new XmlRpcClient ("http://127.0.0.1:9080/xwiki/testbin/xmlrpc/confluence");
        Vector loginParams = new Vector (2);
        loginParams.add ("Admin");
        loginParams.add ("admin");
        try {
          loginToken = (String) rpcClient.execute ("confluence1.login", loginParams);
        } catch (Exception e) {
          loginToken = null;
        }
    }

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public String getHibpath() {
        // Usefull in case we need to understand where we are
        String path = (new File(".")).getAbsolutePath();
        System.out.println("Current Directory is: " + path);

        File file = new File(hibpath);
        if (file.exists())
            return hibpath;

        file = new File("WEB-INF", hibpath);
        if (file.exists())
            return "./WEB-INF/" + hibpath;

        file = new File("test", hibpath);
        if (file.exists())
            return "./test/" + hibpath;

        return hibpath;
    }

    public void tearDown () throws Exception
    {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;

        Vector params = new Vector (1);
        params.add (loginToken);
        rpcClient.execute ("confluence1.logout", params);
        super.tearDown();
    }


    public void testGetPage () throws Exception
    {
        Vector args = new Vector(2);
        args.add (loginToken);
        args.add ("Main.XmlRpcTest");
        Map result = (Map) rpcClient.execute ("confluence1.getPage", args);
        assertEquals ("Main.XmlRpcTest", result.get("title"));
        // assuming there is some content inside the sandbox homepage:
        assertTrue(((String)result.get("content")).length()>0);
    }

    public void testAttachments () throws Exception
    {
        Vector args = new Vector(2);
        args.add (loginToken);
        args.add ("Main.XmlRpcTest");
        // throws exception on next line, if there is at least on attachment:
        Collection result = (Collection) rpcClient.execute ("confluence1.getAttachments", args);
        // assuming we have exactly one attachment:
        assertEquals (1, result.size());
    }

}

