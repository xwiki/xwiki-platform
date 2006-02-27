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
 * @author ludovic
 * @author erwan
 * @author vmassol
 * @author sdumitriu
 */


package com.xpn.xwiki.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebRequest;
import org.apache.struts.action.ActionServlet;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;


public abstract class ServletTest extends ServletTestCase {
    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context = new XWikiContext();
    public XWiki xwiki;

    public void setUp() throws Exception {
        super.setUp();
        flushCache();
    };

    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheStoreInterface)
            return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void cleanUp() {
    };

    public void clientSetUp(XWikiStoreInterface store) throws XWikiException {
    	String configpath = "./xwiki.cfg";
    	XWikiConfig config;
    	try {
    		config = new XWikiConfig(new FileInputStream(configpath));
    	} catch (FileNotFoundException e) {
            Object[] args = { configpath };
    		throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
    	               XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND,
    	               "Configuration file {0} not found", e, args);
    	}
        xwiki = new XWiki(config, context);
        context.setWiki(xwiki);
    }

    public void clientTearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        System.gc();
    }

    public static void setUrl(WebRequest webRequest, String action, String docname) {
        setUrl(webRequest, action, docname, "");
    }

    public static void setUrl(WebRequest webRequest, String action, String docname, String query) {
        setUrl(webRequest, action, "Main", docname, query);
    }

    public static void setUrl(WebRequest webRequest, String action, String web, String docname, String query) {
        webRequest.setURL("127.0.0.1:9080", "/xwiki" , "/testbin", "/" + action + "/" + web + "/" + docname, query);
    }

    public static void setVirtualUrl(WebRequest webRequest, String host, String appname, String action, String docname, String query) {
        webRequest.setURL(host + ":9080", "/" + appname , "/testbin", "/" + action + "/Main/" + docname, query);
    }

    public static void setSimpleUrl(WebRequest webRequest, String web, String docname, String query) {
        webRequest.setURL("127.0.0.1:9080", "/xwiki", "", "/" + web + "/" + docname, query);
    }

    public static void setVerySimpleUrl(WebRequest webRequest, String web, String docname, String query) {
        if (web.equals("Main"))
            webRequest.setURL("127.0.0.1:9080", "/xwiki", "", "/" + docname, query);
        else
            webRequest.setURL("127.0.0.1:9080", "/xwiki", "", "/" + web + "/" + docname, query);
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

        if (config!=null)
        {
            ServletContext context = config.getServletContext();
            if (context!=null)
                return context.getRealPath("WEB-INF/" + hibpath);
        }

        return hibpath;
    }

    public void cleanSession(HttpSession session) {
        Vector names = new Vector();
        Enumeration enumeration = session.getAttributeNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            names.add(name);
        }

        for (int i=0;i<names.size();i++)
        {
            session.removeAttribute((String)names.get(i));
        }
    }

    public void flushCache() {
        // We need to flush the server cache before running our tests
        // because we are modifiying the database behind the scenes
        // so if we are running the tests twice we won't necessarly
        // get the same results..
        try {
            XWiki xwiki = (XWiki) config.getServletContext().getAttribute("xwikitest");
            if (xwiki!=null)
                xwiki.flushCache();
            xwiki = (XWiki) config.getServletContext().getAttribute("xwiki");
            if (xwiki!=null)
             xwiki.flushCache();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void launchTest() throws Throwable {

      // by default we don't check the number of active connections
      launchTest(false);
    }

    public void launchTest(boolean checkActive) throws Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);

            if (checkActive) {
                // Let's verify that we didn't let any connections behind..
                XWiki xwiki = (XWiki) config.getServletContext().getAttribute("xwiki");
                if (xwiki != null) {
                    XWikiHibernateStore store = (XWikiHibernateStore) ((XWikiCacheStoreInterface) xwiki.getStore()).getStore();
                    assertEquals("Active connections in xwiki should be zero", 0, store.getConnections().size());
                }

                xwiki = (XWiki) config.getServletContext().getAttribute("xwikitest");
                if (xwiki != null) {
                    XWikiHibernateStore store = (XWikiHibernateStore) ((XWikiCacheStoreInterface) xwiki.getStore()).getStore();
                    assertEquals("Active connections in xwikitest should be zero", 0, store.getConnections().size());
                }
            }

            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }
    }

}
