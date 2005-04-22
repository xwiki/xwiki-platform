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
 * Date: 21 avr. 2005
 * Time: 08:59:39
 */
package com.xpn.xwiki.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class AllTests {
    public static String hibpath = "hibernate-test.cfg.xml";

    private static void cleanUp() throws XWikiException {
        XWikiContext context = new XWikiContext();
        context.setDatabase("xwikitest");
        //XWiki xwiki = new XWiki("./xwiki.cfg", context);
        //context.setWiki(xwiki);
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        StoreHibernateTest.cleanUp(hibstore, true, true, context);
    }

    public static Test suite () throws XWikiException {
        TestSuite suite= new TestSuite("Test for com.xpn.xwiki");
        cleanUp();

        //$JUnit-BEGIN$
        suite.addTestSuite(XWikiTest.class);
        suite.addTestSuite(ClassesTest.class);
        suite.addTestSuite(i18nTest.class);
        suite.addTestSuite(MonitorTest.class);
        suite.addTestSuite(ObjectTest.class);
        suite.addTestSuite(PluginTest.class);
        suite.addTestSuite(RadeoxRenderTest.class);
        suite.addTestSuite(VelocityRenderTest.class);
        suite.addTestSuite(WikiWikiBaseRenderTest.class);
        suite.addTestSuite(StatsTest.class);
        suite.addTestSuite(StoreRCSFileTest.class);
        suite.addTestSuite(StoreHibernateTest.class);
        suite.addTestSuite(StoreHibernateCacheTest.class);
        suite.addTestSuite(StoreObjectRCSFileTest.class);
        suite.addTestSuite(StoreObjectHibernateTest.class);
        suite.addTestSuite(SearchTest.class);
        suite.addTestSuite(UserTest.class);
        suite.addTestSuite(UtilTest.class);
        suite.addTestSuite(PDFExportTest.class);
        suite.addTestSuite(LDAPTest.class);
        suite.addTestSuite(PerlTest.class);
        //$JUnit-END$

        return suite;
    }
}
