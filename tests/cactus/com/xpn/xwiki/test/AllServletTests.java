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

import junit.framework.Test;
import junit.framework.TestSuite;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

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
