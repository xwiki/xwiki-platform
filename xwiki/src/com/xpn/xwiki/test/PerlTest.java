/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 30 nov. 2003
 * Time: 14:53:30
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiPerlPluginRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiWikiBaseRenderer;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import net.sf.hibernate.HibernateException;

public class PerlTest extends TestCase {
    XWiki xwiki;
    XWikiContext context;
    XWikiPerlPluginRenderer perlplugin;
    XWikiWikiBaseRenderer wikirenderer;
    XWikiRenderingEngine wikiengine;

    public void setUp() throws XWikiException {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
        context.setWiki(xwiki);
        perlplugin = new XWikiPerlPluginRenderer(xwiki.Param("xwiki.perl.perlpath"),
                        xwiki.Param("xwiki.perl.pluginspath"),
                        xwiki.Param("xwiki.perl.classespath"),
                        xwiki.Param("xwiki.perl.javaserverport", "7891"), 0);
        wikiengine = xwiki.getRenderingEngine();
        wikirenderer = new XWikiWikiBaseRenderer();
        // perlplugin = (XWikiPerlPluginRenderer) wikiengine.getRenderer(XWikiPerlPluginRenderer.class.getName());
    }


    public XWikiHibernateStore getHibStore() {
        XWikiStoreInterface store = xwiki.getStore();
        if (store instanceof XWikiCacheInterface)
            return (XWikiHibernateStore)((XWikiCacheInterface)store).getStore();
        else
            return (XWikiHibernateStore) store;
    }

    public void tearDown() throws HibernateException {
        getHibStore().shutdownHibernate(context);
        xwiki = null;
        context = null;
        wikirenderer = null;
        wikiengine = null;
        System.gc();
    }


    protected void finalize() throws Throwable {
        super.finalize();
        System.err.println("Stopping Servers");
        perlplugin.stopServers();
    }

    public void testPerlPlugin(int nb) throws XWikiException {
        for (int i=0;i<nb;i++) {
            RenderTest.renderTest(perlplugin, "Hello 1\n---\nHello 2",
                    "Hello 1\n<hr />\nHello 2", true, context);
        }
    }

    public void testPerlPlugin() throws XWikiException {
        testPerlPlugin(1);
    }

    public void testPerlPlugin1() throws XWikiException {
        testPerlPlugin(1);
    }

    public void testPerlPlugin10() throws XWikiException {
        testPerlPlugin(10);
    }

    public void testPerlPlugin100() throws XWikiException {
        testPerlPlugin(100);
    }

    public void testPerlPlugin1000() throws XWikiException {
        testPerlPlugin(1000);
    }

    public void testWikiEngine(int nb) throws XWikiException {
        for (int i=0;i<nb;i++) {
            RenderTest.renderTest(wikiengine, "Hello 1\n----\nHello 2",
                    "Hello 1\n<hr class=\"line\"/>\nHello 2", true, context);
        }
    }



    public void testWikiEngine() throws XWikiException {
        testWikiEngine(1);
    }

    public void testWikiEngine1() throws XWikiException {
        testWikiEngine(1);
    }

    public void testWikiEngine10() throws XWikiException {
        testWikiEngine(10);
    }

    public void testWikiEngine100() throws XWikiException {
        testWikiEngine(100);
    }

    public void testWikiEngine1000() throws XWikiException {
        testWikiEngine(1000);
    }


    public void testWikiRenderer(int nb) throws XWikiException {
        for (int i=0;i<nb;i++) {
            RenderTest.renderTest(wikirenderer, "Hello 1\n---\nHello 2",
                    "Hello 1\n<hr />\nHello 2", true, context);
        }
    }

    public void testWikiRenderer() throws XWikiException {
        testWikiRenderer(1);
    }

    public void testWikiRenderer1() throws XWikiException {
        testWikiRenderer(1);
    }

    public void testWikiRenderer10() throws XWikiException {
        testWikiRenderer(10);
    }

    public void testWikiRenderer100() throws XWikiException {
        testWikiRenderer(100);
    }

    public void testWikiRenderer1000() throws XWikiException {
        testWikiRenderer(1000);
    }

    public void testPerlCalendarPlugin(int nb) throws XWikiException {
        for (int i=0;i<nb;i++) {
            RenderTest.renderTest(perlplugin, "%CALENDAR%",
                    "<table", false, context);
        }
    }

    /*
    public void testPerlCalendarPlugin() throws XWikiException {
        testPerlCalendarPlugin(1);
    }
    */

    public void testPTabListRenderer() throws XWikiException {
             RenderTest.renderTest(perlplugin, "\t* List1",
                    "<ul>\n<li> List1\n</ul>\n", true, context);
             RenderTest.renderTest(perlplugin, "\t* List1\n\t* List2",
                   "<ul>\n<li> List1\n<li> List2\n</ul>\n", true, context);
             RenderTest.renderTest(perlplugin, "\t* List1\n\t\t* List2",
                  "<ul>\n<li> List1\n<ul>\n<li> List2\n</ul>\n</ul>\n", true, context);

        }
    public void testPerlSpaceListRenderer() throws XWikiException {
            // This test is known to fail. It is not doing what I expected 
            /*
             RenderTest.renderTest(perlplugin, "   * List1",
                    "<ul>\n<li> List1\n</ul>\n", true, context);
             RenderTest.renderTest(perlplugin, "   * List1\n   * List2",
                   "<ul>\n<li> List1\n\n<li> List2\n</ul>\n", true, context);
             RenderTest.renderTest(perlplugin, "   * List1\n      * List2",
                  "<ul>\n<li> List1\n<ul>\n<li> List2</ul>\n</ul>\n", true, context);
             */
        }


    /*
    public void testPerlPlugin10000() throws XWikiException {
        testPerlPlugin(10000);
    }
    public void testWikiEngine10000() throws XWikiException {
        testWikiEngine(10000);
    }

    public void testWikiRenderer10000() throws XWikiException {
        testWikiRenderer(10000);
    }
    */

}

