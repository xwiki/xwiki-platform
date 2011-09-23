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
 */
package org.xwiki.wysiwyg.server.internal.cleaner;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.lang.reflect.Method;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.XWikiComponentInitializer;

/**
 * JUnit Test Setup that initializes the Component Manager.
 * 
 * @version $Id$
 * @since 2.0M1
 * @deprecated use JUnit4's @RunWith instead of the older JUnit3 Test Suite/Test Setup notion
 */
public class ComponentManagerTestSetup extends TestSetup
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    private List<ComponentDescriptor< ? >> componentDescriptors = new ArrayList<ComponentDescriptor< ? >>();

    public ComponentManagerTestSetup(TestSuite suite)
    {
        super(suite);
    }

    public ComponentManagerTestSetup(TestSuite suite, List<ComponentDescriptor< ? >> componentDescriptors)
        throws Exception
    {
        super(suite);
        this.componentDescriptors.addAll(componentDescriptors);
    }

    public void addComponentDescriptor(ComponentDescriptor< ? > componentDescriptor)
    {
        this.componentDescriptors.add(componentDescriptor);    
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.extensions.TestDecorator#getTest()
     */
    @Override
    public TestSuite getTest()
    {
        return (TestSuite) super.getTest();
    }

    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) which can
     *         then be put in the XWiki Context for testing.
     * @since 1.7M3
     */
    public EmbeddableComponentManager getComponentManager() throws Exception
    {
        return this.initializer.getComponentManager();
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.extensions.TestSetup#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        this.initializer.initializeConfigurationSource();

        // Register defined component descriptors as components
        for (ComponentDescriptor< ? > descriptor : this.componentDescriptors) {
            getComponentManager().registerComponent(descriptor);
        }

        this.initializer.initializeExecution();

        for (Enumeration<Test> tests = getTest().tests(); tests.hasMoreElements();) {
            Test test = tests.nextElement();
            try {
                Method method = test.getClass().getMethod("setComponentManager", ComponentManager.class);
                method.invoke(test, this.initializer.getComponentManager());
            } catch (Exception e) {
                // Apparently the test doesn't have a setComponentManager method, don't do anything then.
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.extensions.TestSetup#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }
}
