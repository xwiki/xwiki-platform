package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.cactus.client.authentication.Authentication;
import org.apache.cactus.client.authentication.BasicAuthentication;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 13 mars 2004
 * Time: 15:29:22
 * To change this template use File | Settings | File Templates.
 */
public class ServletVirtualTest extends ServletTest {

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(xwiki, context, fullname, user, group, level, allow, global);
    }

    public void beginVirtualViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest", context);
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        setVirtualUrl(webRequest, "127.0.0.1", "xwiki", "view", "VirtualViewOkTest", "");
    }

    public void endVirtualViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testVirtualViewOk() throws Throwable {
        launchTest();
    }

     public void beginVirtualView2(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());

        // Setup database xwikitest
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        // Setup database xwikitest2
        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest2", context);

        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualViewOkTest2", "");
    }

    public void endVirtualView2(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testVirtualView2() throws Throwable {
        launchTest();
    }


    public void beginAuth(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);

        // Create User in the virtual wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, context);

        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthTest", context);
        updateRight("Main.VirtualAuthTest", "XWiki.LudovicDubost", "", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthTest", "");
        Authentication auth = new BasicAuthentication("LudovicDubost", "toto");
        webRequest.setAuthentication(auth);
    }

    public void endAuth(WebResponse webResponse) {
        assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testAuth() throws Throwable {
        launchTest();
    }


    public void beginAuthForWikiOwner(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthOwnerTest", context);
        updateRight("Main.VirtualAuthOwnerTest", "XWiki.LudovicDubost", "", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthOwnerTest", "");
        Authentication auth = new BasicAuthentication("LudovicDubost", "toto");
        webRequest.setAuthentication(auth);
    }

    public void endAuthForWikiOwner(WebResponse webResponse) {
        assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testAuthForWikiOwner() throws Throwable {
        launchTest();
    }



}
