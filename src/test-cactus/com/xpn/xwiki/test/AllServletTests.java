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
 * Date: 23 avr. 2005
 * Time: 01:24:02
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllServletTests {
        public static String hibpath = "hibernate-test.cfg.xml";

        private static void cleanUp() throws XWikiException {
            XWikiContext context = new XWikiContext();
            context.setDatabase("xwikitest");
            XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
            StoreHibernateTest.cleanUp(hibstore, true, true, context);
        }

        public static Test suite () throws XWikiException {
            TestSuite suite= new TestSuite("Servlet Test for com.xpn.xwiki");
            cleanUp();

            //$JUnit-BEGIN$
            suite.addTestSuite(ViewEditTest.class);
            suite.addTestSuite(ServletAuthTest.class);
            suite.addTestSuite(Servleti18nTest.class);
            suite.addTestSuite(ServletStatsTest.class);
            suite.addTestSuite(ServletVirtualTest.class);
            suite.addTestSuite(XMLRpcTest.class);
            //$JUnit-END$

            return suite;
        }
    }
