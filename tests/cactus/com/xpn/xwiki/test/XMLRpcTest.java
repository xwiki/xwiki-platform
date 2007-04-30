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
import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.xmlrpc.ConfluenceRpcInterface;
import com.xpn.xwiki.xmlrpc.XWikiRpcInterface;

public class XMLRpcTest  extends TestCase {
	private static final Log log = LogFactory.getLog(XMLRpcTest.class);
	
    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context = new XWikiContext();
    public XWiki xwiki;
    private XmlRpcClient rpcClient;
    private String token;

	private ConfluenceRpcInterface xwikiRpc;

	private String pageId;

    public XMLRpcTest ()
    {
        super();
    }

    public void setUp() throws Exception {
        super.setUp();
    	String configpath = "./xwiki.cfg";
    	pageId = "Main.XmlRpcTest";
		XWikiConfig config = new XWikiConfig(new FileInputStream(configpath));
        xwiki = new XWiki(config, context);

        context.setWiki(xwiki);

        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "XmlRpcTest", context);

        XWikiDocument doc1 = xwiki.getDocument(pageId, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);

        HashMap map = new HashMap();
        map.put("password", "admin");
        xwiki.createUser("Admin", map, "", "", "view, edit", context);

        rpcClient = new XmlRpcClient();
        XmlRpcClientConfigImpl clientConfig = new XmlRpcClientConfigImpl();
        clientConfig.setServerURL(new URL("http://127.0.0.1:9080/xwiki/xmlrpc"));
        rpcClient.setConfig(clientConfig);
        
        ClientFactory factory = new ClientFactory(rpcClient);
        xwikiRpc = (ConfluenceRpcInterface)
                		factory.newInstance(ConfluenceRpcInterface.class);
		token = (String) xwikiRpc.login("Admin", "admin");
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
        params.add (token);
        rpcClient.execute ("confluence1.logout", params);
        super.tearDown();
    }


    public void testGetPage () throws Exception
    {
        Map result = (Map)xwikiRpc. getPage(token, pageId);
        assertEquals (pageId, result.get("title"));
        // assuming there is some content inside the sandbox homepage:
        assertTrue(((String)result.get("content")).length()>0);
    }

    public void testAttachments () throws Exception
    {
        Object[] result = (Object[])xwikiRpc.getAttachments(token, pageId);
        // assuming we have exactly one attachment:
        assertEquals (1, result.length);
        assertTrue(Map.class.isInstance(result[0]));
        Map attachment = (Map)result[0];
        assertEquals("test1.sxw", attachment.get("fileName"));
        assertEquals("test1.sxw", attachment.get("id"));
        assertEquals("Main.XmlRpcTest", attachment.get("pageId"));
    }
    
    public void testGetSpaces() throws Exception {
    	Object[] spaces = xwikiRpc.getSpaces(token);
    	for (int i = 0; i<spaces.length; i++) {
    		Map space = (Map)spaces[i];
    		String name = (String)space.get("name");
    		Map sameSpace = xwikiRpc.getSpace(token, (String)space.get("key"));
    		assertEquals(space.get("name"), sameSpace.get("name"));
    	}
    }
}
