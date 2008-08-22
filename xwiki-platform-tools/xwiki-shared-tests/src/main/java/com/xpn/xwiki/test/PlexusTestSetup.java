/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.test;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.Enumeration;
import java.lang.reflect.Method;

import org.xwiki.component.manager.ComponentManager;

/**
 * JUnit Test Setup that initializes Plexus. Test suite that want to initialize the Plexus component manager
 * only once should use this class instead of {@link AbstractXWikiComponentTestCase}.
 *
 * @version $Id: $
 * @since 1.6M1
 */
public class PlexusTestSetup extends TestSetup
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    public PlexusTestSetup(TestSuite suite)
    {
        super(suite);
    }

    public TestSuite getTest()
    {
        return (TestSuite) super.getTest();
    }

    protected void setUp() throws Exception
    {
        this.initializer.initialize();
        for (Enumeration tests = getTest().tests(); tests.hasMoreElements();) {
            Test test = (Test) tests.nextElement();
            try {
                Method method = test.getClass().getMethod("setComponentManager", ComponentManager.class);
                method.invoke(test, this.initializer.getComponentManager());
            } catch (Exception e) {
                // Apparently the test doesn't have a setComponentManager method, don't do anything then.
            }
        }
    }

    protected void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }
}
