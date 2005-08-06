package com.xpn.xwiki.test;

import java.io.FileInputStream;
import java.net.URL;

import org.apache.cactus.ServletTestCase;
import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public abstract class XWikiIntegrationTest extends ServletTestCase {
  protected XWiki xwiki;
  protected XWikiContext context;

  protected XWikiHibernateStore getHibStore() {
    XWikiStoreInterface store = xwiki.getStore();
    if (store instanceof XWikiCacheStoreInterface)
        return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
    else
        return (XWikiHibernateStore) store;
}

  protected XWikiStoreInterface getStore() {
      return xwiki.getStore();
  }
  
  public void setUp() throws Exception {
      context = new XWikiContext();
      context.setDatabase("xwikitest");
      String configpath = "./test/xwiki-integration.cfg";
	  XWikiConfig config = new XWikiConfig(new FileInputStream(configpath));
      xwiki = new XWiki(config, context);

      xwiki.setDatabase("xwikitest");
      
      context.setWiki(xwiki);
      context.setURLFactory(new XWikiServletURLFactory(new URL("http://localhost:8080/"), "xwiki/" , "bin/"));
      Velocity.init("test/velocity.properties");

      context.setRequest(new XWikiServletRequest(request));
      context.setResponse(new XWikiServletResponse(response));
//      XWikiContext context = Utils.prepareContext(action, request, response, new XWikiServletContext(servlet.getServletContext()));
  }
  
  public void tearDown() throws HibernateException {
      getHibStore().shutdownHibernate(context);
      xwiki = null;
      context = null;
      System.gc();
  }
}
