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
package org.xwiki.test;

import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.component.embed.EmbeddableComponentManager;

/**
 * Tests which needs to have XWiki Components set up should extend this class which makes the Component Manager
 * available. Use this class for JUnit 3.x tests. For Junit 4.x tests use {@link AbstractComponentTestCase} instead.
 */
public abstract class AbstractXWikiComponentTestCase extends MockObjectTestCase
{
    private XWikiComponentInitializer initializer = new XWikiComponentInitializer();

    public AbstractXWikiComponentTestCase()
    {
        super();
    }

    public AbstractXWikiComponentTestCase(String testName)
    {
        super(testName);
    }

    /**
     * Tests that require fine-grained initializations can override this method and not call super. 
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        this.initializer.initializeContainer();

        // Put before execution context initialization because it could be needed for some executing context
        // initializer
        registerComponents();

        this.initializer.initializeExecution();
    }

    /**
     * Register custom/mock components
     */
    protected void registerComponents() throws Exception
    {
        // Empty voluntarily. Extending classes can override to provide custom component registration.
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        this.initializer.shutdown();
    }

    /**
     * @return a configured Component Manager (which uses the plexus.xml file in the test resources directory) which can
     *         then be put in the XWiki Context for testing.
     */
    public EmbeddableComponentManager getComponentManager() throws Exception
    {
        return this.initializer.getComponentManager();
    }
}
