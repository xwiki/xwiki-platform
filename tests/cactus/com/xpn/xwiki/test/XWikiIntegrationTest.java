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
 * @author vmassol
 * @author sdumitriu
 */
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
      String configpath = "./xwiki.cfg";
	  XWikiConfig config = new XWikiConfig(new FileInputStream(configpath));
      xwiki = new XWiki(config, context);

      xwiki.setDatabase("xwikitest");
      
      context.setWiki(xwiki);
      context.setURLFactory(new XWikiServletURLFactory(new URL("http://127.0.0.1:9080/"), "xwiki/" , "testbin/"));
      Velocity.init("/velocity.properties");

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
