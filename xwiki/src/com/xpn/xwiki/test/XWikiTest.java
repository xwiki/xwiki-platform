package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import net.sf.hibernate.HibernateException;
import org.apache.velocity.app.Velocity;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 6 mars 2004
 * Time: 12:01:47
 * To change this template use File | Settings | File Templates.
 */
public class XWikiTest extends TestCase {

    private XWiki xwiki;
     private XWikiContext context;

     public XWikiHibernateStore getHibStore() {
         XWikiStoreInterface store = xwiki.getStore();
         if (store instanceof XWikiCacheInterface)
             return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
         else
             return (XWikiHibernateStore) store;
     }

     public XWikiStoreInterface getStore() {
         return xwiki.getStore();
     }

     public void setUp() throws Exception {
         context = new XWikiContext();
         xwiki = new XWiki("./xwiki.cfg", context);
         context.setWiki(xwiki);
         Velocity.init("velocity.properties");
         StoreHibernateTest.cleanUp(getHibStore(), context);
     }

     public void tearDown() throws HibernateException {
         getHibStore().shutdownHibernate(context);
         xwiki = null;
         context = null;
         System.gc();
     }

    public void testDefaultSkin() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        RenderTest.renderTest(wikiengine, "$xwiki.getSkin()",
                "default", true, context);
    }

    public void testModifiedSkin() throws XWikiException {
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", context);
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        RenderTest.renderTest(wikiengine, "$xwiki.getSkin()",
                "altern", true, context);
    }


}
